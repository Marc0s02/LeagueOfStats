package com.marclw.lolstats.training;

import com.marclw.lolstats.features.FeatureExtractor;
import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.features.TeamAggregator;
import com.marclw.lolstats.ingest.MatchDataParser;
import com.marclw.lolstats.ingest.MatchFetcher;
import com.marclw.lolstats.ingest.RiotMatchClient;
import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.storage.FeatureStore;
import com.marclw.lolstats.storage.ModelRegistry;
import smile.classification.SoftClassifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Runs the whole offline path end to end, in one command:
 *
 *   fetch matches -> parse -> extract features -> persist feature table
 *   -> split train/test -> train -> evaluate -> save model
 *
 * This is the class that turns all the individual pieces into something
 * that produces an actual, reportable result. Run it, read the accuracy/AUC
 * numbers it prints against the baseline, and you have the core claim the
 * project report needs.
 *
 * Usage:
 *   RIOT_API_KEY=your-key java TrainingPipelineRunner &lt;riotIds&gt; [options]
 *
 *   riotIds           comma-separated gameName#tagLine list, e.g.
 *                     "DaddyYank3e#EUW1,SomeOther#EUW1"
 *   --count=N         matches to pull per account (default 20)
 *   --minute=N        the "as of minute" to extract features at (default 15)
 *   --region=URL      regional base URL (default https://europe.api.riotgames.com)
 *   --out=DIR         where to write the feature table + model (default ./data)
 *   --reuse-features  skip fetching entirely and train on a previously
 *                     written feature table - use this to re-tune the model
 *                     without burning API quota again
 *
 * NOTE ON RUNTIME: a dev key is limited to roughly 100 requests per 2
 * minutes, and each match costs 2 requests. 100 matches therefore takes
 * about 4 minutes of mostly waiting. That's why --reuse-features exists:
 * fetch once, train many times.
 */
public class TrainingPipelineRunner {

    private static final String FEATURE_TABLE_FILE = "features.csv";
    private static final double TRAIN_FRACTION = 0.8;
    /** Fixed seed so a re-run on the same data gives the same split - makes
     *  "did my change help?" comparisons meaningful rather than noise. */
    private static final long SPLIT_SEED = 42L;

    public static void main(String[] args) {
        List<String> riotIds = new ArrayList<>();
        int countPerAccount = 20;
        int minute = 15;
        String regionalBaseUrl = "https://europe.api.riotgames.com";
        Path outputDirectory = Path.of("data");
        boolean reuseFeatures = false;

        for (String arg : args) {
            if (arg.startsWith("--count=")) {
                countPerAccount = Integer.parseInt(value(arg));
            } else if (arg.startsWith("--minute=")) {
                minute = Integer.parseInt(value(arg));
            } else if (arg.startsWith("--region=")) {
                regionalBaseUrl = value(arg);
            } else if (arg.startsWith("--out=")) {
                outputDirectory = Path.of(value(arg));
            } else if (arg.equals("--reuse-features")) {
                reuseFeatures = true;
            } else if (!arg.startsWith("--")) {
                riotIds.addAll(Arrays.asList(arg.split(",")));
            } else {
                System.err.println("Unknown option: " + arg);
                System.exit(1);
            }
        }

        if (riotIds.isEmpty() && !reuseFeatures) {
            System.err.println("Usage: TrainingPipelineRunner <gameName#tagLine[,...]> "
                    + "[--count=N] [--minute=N] [--region=URL] [--out=DIR] [--reuse-features]");
            System.exit(1);
        }

        try {
            Files.createDirectories(outputDirectory);
        } catch (Exception e) {
            System.err.println("Could not create output directory " + outputDirectory + ": " + e.getMessage());
            System.exit(1);
        }

        FeatureStore featureStore = new FeatureStore(outputDirectory.resolve(FEATURE_TABLE_FILE));
        List<FeatureVector> allRows;

        if (reuseFeatures) {
            System.out.println("Reusing existing feature table at " + featureStore.getStorageLocation());
            allRows = featureStore.readFeatureTable();
        } else {
            String apiKey = System.getenv("RIOT_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                System.err.println("RIOT_API_KEY environment variable is not set.");
                System.exit(1);
                return; // unreachable, but keeps the compiler happy about apiKey below
            }

            RiotMatchClient matchClient = new RiotMatchClient(apiKey, regionalBaseUrl);
            MatchFetcher fetcher = new MatchFetcher(matchClient, new MatchDataParser());

            int expectedMatches = riotIds.size() * countPerAccount;
            System.out.printf("Fetching up to %d matches across %d account(s)."
                            + " At ~%.1fs per match this takes roughly %d minute(s).%n",
                    expectedMatches, riotIds.size(),
                    fetcher.getThrottleMillis() / 1000.0,
                    Math.max(1, (expectedMatches * fetcher.getThrottleMillis()) / 60000));

            MatchFetcher.Batch batch = fetcher.fetchRecentMatchesForRiotIds(riotIds, countPerAccount);
            System.out.println("Fetched " + batch.size() + " unique matches.");

            FeatureExtractor featureExtractor =
                    new FeatureExtractor(new TeamAggregator(), new StatCalculator());
            DatasetBuilder datasetBuilder = new DatasetBuilder(featureExtractor);

            allRows = datasetBuilder.buildTrainingSet(
                    batch.getRecords(), batch.getTimelines(), minute);
            System.out.println("Built " + allRows.size() + " labeled feature rows at minute " + minute + ".");

            featureStore.writeFeatureTable(allRows);
            System.out.println("Wrote feature table to " + featureStore.getStorageLocation());
        }

        if (allRows.size() < 20) {
            System.err.println("Only " + allRows.size() + " rows - too few to train anything "
                    + "meaningful. Pull more matches (raise --count, or add more accounts).");
            System.exit(1);
        }

        // --- Split -------------------------------------------------------
        // Shuffle before splitting: match IDs come back newest-first, so an
        // unshuffled split would put all the oldest games in the test set and
        // quietly turn this into a "does the model generalise across patches"
        // experiment instead of the intended one.
        List<FeatureVector> shuffled = new ArrayList<>(allRows);
        Collections.shuffle(shuffled, new Random(SPLIT_SEED));
        int trainSize = (int) Math.round(shuffled.size() * TRAIN_FRACTION);
        List<FeatureVector> trainRows = shuffled.subList(0, trainSize);
        List<FeatureVector> testRows = shuffled.subList(trainSize, shuffled.size());
        System.out.printf("Split: %d training rows, %d held-out test rows.%n",
                trainRows.size(), testRows.size());

        // --- Train -------------------------------------------------------
        ModelTrainer trainer = new ModelTrainer();
        SoftClassifier<double[]> model = trainer.trainLogisticRegression(trainRows);
        System.out.println("Trained logistic regression on FeatureSpec " + FeatureSpec.VERSION + ".");

        // --- Evaluate ----------------------------------------------------
        ModelEvaluator evaluator = new ModelEvaluator();
        double accuracy = evaluator.accuracy(model, testRows);
        double auc = evaluator.areaUnderRocCurve(model, testRows);
        double baseline = evaluator.baselineAccuracy(testRows);

        System.out.println();
        System.out.println("=== Evaluation (held-out test set) ===");
        System.out.printf("Accuracy:           %.4f%n", accuracy);
        System.out.printf("AUC:                %.4f%n", auc);
        System.out.printf("Baseline (blue):    %.4f%n", baseline);
        System.out.printf("Lift over baseline: %+.4f%n", accuracy - baseline);
        System.out.println();
        System.out.println("Read these honestly: an AUC near 0.5 means the model is guessing. "
                + "Accuracy below the baseline means it is actively worse than always "
                + "picking blue. Both are normal on a small corpus - the fix is more "
                + "matches, not a fancier model.");

        // --- Persist -----------------------------------------------------
        ModelRegistry registry = new ModelRegistry(outputDirectory.resolve("model"));
        registry.saveModel(model, FeatureSpec.VERSION);
        System.out.println();
        System.out.println("Saved model to " + registry.getModelDirectory());
    }

    private static String value(String arg) {
        return arg.substring(arg.indexOf('=') + 1);
    }
}