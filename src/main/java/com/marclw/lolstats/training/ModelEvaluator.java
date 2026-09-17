package com.marclw.lolstats.training;

import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.prediction.ModelScorer;
import smile.classification.Classifier;
import smile.validation.metric.AUC;

import java.util.List;

/**
 * Offline path, step 3: measures a trained model's quality against a
 * held-out test set (rows DatasetBuilder produced but ModelTrainer never
 * saw). This is the piece that turns "I trained a model" into an actual,
 * defensible claim - worth keeping the numbers (and the baseline
 * comparison below) for the project report.
 */
public class ModelEvaluator {

    /**
     * @param model     from ModelTrainer
     * @param testRows  held-out FeatureVectors with blueTeamWon set,
     *                  not used during training
     */
    public double accuracy(Classifier<double[]> model, List<FeatureVector> testRows) {
        if (testRows.isEmpty()) {
            return 0;
        }
        int correct = 0;
        for (FeatureVector row : testRows) {
            boolean predictedBlueWin = ModelScorer.blueWinProbability(model, row) >= 0.5;
            if (predictedBlueWin == requireLabel(row)) {
                correct++;
            }
        }
        return (double) correct / testRows.size();
    }

    /**
     * Area under the ROC curve - more informative than raw accuracy for a
     * roughly-balanced win/loss classification problem, since it measures
     * ranking quality across all thresholds rather than just at 0.5.
     */
    public double areaUnderRocCurve(Classifier<double[]> model, List<FeatureVector> testRows) {
        if (testRows.isEmpty()) {
            return 0;
        }
        int[] truth = new int[testRows.size()];
        double[] probability = new double[testRows.size()];
        for (int i = 0; i < testRows.size(); i++) {
            FeatureVector row = testRows.get(i);
            truth[i] = requireLabel(row) ? 1 : 0;
            probability[i] = ModelScorer.blueWinProbability(model, row);
        }
        return AUC.of(truth, probability);
    }

    /**
     * Accuracy of the trivial "blue side always wins" baseline on the same
     * test set, so the real model's accuracy can be reported as an
     * improvement over it rather than a bare number. Blue side genuinely
     * does win slightly more than half of ranked games, so this baseline
     * is usually a touch above 0.50 - a model that only matches it has
     * learned nothing useful.
     */
    public double baselineAccuracy(List<FeatureVector> testRows) {
        if (testRows.isEmpty()) {
            return 0;
        }
        int blueWins = 0;
        for (FeatureVector row : testRows) {
            if (requireLabel(row)) {
                blueWins++;
            }
        }
        return (double) blueWins / testRows.size();
    }

    private boolean requireLabel(FeatureVector row) {
        Boolean blueWon = row.getBlueTeamWon();
        if (blueWon == null) {
            throw new IllegalArgumentException(
                    "Test row has no blueTeamWon label - can't evaluate against it");
        }
        return blueWon;
    }
}