package com.marclw.lolstats.prediction;

import com.marclw.lolstats.features.FeatureExtractor;
import com.marclw.lolstats.ingest.MatchDataParser;
import com.marclw.lolstats.ingest.RiotMatchClient;
import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;
import com.marclw.lolstats.storage.ModelRegistry;
import smile.classification.Classifier;

/**
 * Online path: what actually runs when a user asks for a prediction from
 * PredictionController. Deliberately thin - all the real logic (feature
 * computation) lives in features.FeatureExtractor and is shared with the
 * offline training path; this class just wires fetch -&gt; extract -&gt; load
 * model -&gt; score together.
 *
 * The FeatureExtractor instance handed in here MUST be equivalent to the
 * one DatasetBuilder trained with (same TeamAggregator/StatCalculator
 * wiring). App.java constructs one and passes it to both - don't build a
 * second copy here, or training/serving can silently drift apart.
 */
public class PredictionService {

    private final ModelRegistry modelRegistry;
    private final RiotMatchClient matchClient;
    private final MatchDataParser parser;
    private final FeatureExtractor featureExtractor;

    /**
     * Lazily loaded and then cached: loading deserializes the model from
     * disk and verifies its FeatureSpec version, which is wasted work on
     * every single prediction. Not thread-safe by design - JavaFX calls
     * this from the application thread.
     */
    private Classifier<double[]> cachedModel;

    public PredictionService(ModelRegistry modelRegistry,
                             RiotMatchClient matchClient,
                             MatchDataParser parser,
                             FeatureExtractor featureExtractor) {
        this.modelRegistry = modelRegistry;
        this.matchClient = matchClient;
        this.parser = parser;
        this.featureExtractor = featureExtractor;
    }

    /**
     * @param matchId a completed match ID (e.g. "EUW1_7975080416")
     * @param minute  the point in the game to predict "as of"
     *
     * Note this currently only handles FINISHED matches, via Match-V5.
     * Live games would need RiotSpectatorClient, but Spectator-V5 exposes
     * only champion picks and elapsed time - no gold/XP/objective counts -
     * so the features this model was trained on simply aren't available
     * for an in-progress game. Predicting a finished match "as of minute
     * 15" is still a genuine demonstration of the model, it just isn't
     * live. Worth being straight about that distinction in the report
     * rather than implying live prediction works.
     */
    public WinProbabilityResult predict(String matchId, int minute) {
        String matchJson = matchClient.fetchMatch(matchId);
        String timelineJson = matchClient.fetchMatchTimeline(matchId);

        MatchRecord record = parser.parseMatchRecord(matchJson);
        MatchTimeline timeline = parser.parseMatchTimeline(timelineJson, matchJson);

        // blueTeamWon is deliberately left unset on this vector: at serving
        // time the outcome is what we're predicting, not an input. (For a
        // finished match we technically know it, but feeding it in would
        // make the prediction meaningless.)
        FeatureVector vector = featureExtractor.extract(record, timeline, minute);

        double blueWinProbability = ModelScorer.blueWinProbability(model(), vector);
        return new WinProbabilityResult(matchId, minute, blueWinProbability);
    }

    /**
     * Predicts at every minute from 1 up to the requested one, so the UI can
     * draw win probability over time rather than a single number. Cheap -
     * the match and timeline are fetched once and re-scored per minute.
     */
    public WinProbabilityResult[] predictOverTime(String matchId, int throughMinute) {
        String matchJson = matchClient.fetchMatch(matchId);
        String timelineJson = matchClient.fetchMatchTimeline(matchId);

        MatchRecord record = parser.parseMatchRecord(matchJson);
        MatchTimeline timeline = parser.parseMatchTimeline(timelineJson, matchJson);

        WinProbabilityResult[] results = new WinProbabilityResult[throughMinute];
        for (int minute = 1; minute <= throughMinute; minute++) {
            FeatureVector vector = featureExtractor.extract(record, timeline, minute);
            results[minute - 1] = new WinProbabilityResult(
                    matchId, minute, ModelScorer.blueWinProbability(model(), vector));
        }
        return results;
    }

    /**
     * How many minutes of timeline data a match actually has - so the UI can
     * bound its minute selector to something real rather than letting a user
     * ask for minute 45 of a 22-minute game (which would silently score the
     * last available frame over and over).
     */
    public int availableMinutes(String matchId) {
        String matchJson = matchClient.fetchMatch(matchId);
        String timelineJson = matchClient.fetchMatchTimeline(matchId);
        MatchTimeline timeline = parser.parseMatchTimeline(timelineJson, matchJson);
        return timeline.getFrames() == null ? 0 : timeline.getFrames().size() - 1;
    }

    private Classifier<double[]> model() {
        if (cachedModel == null) {
            cachedModel = modelRegistry.loadModel();
        }
        return cachedModel;
    }

    /**
     * Drops the cached model so the next prediction reloads from disk.
     * Useful after retraining without restarting the app.
     */
    public void invalidateModelCache() {
        cachedModel = null;
    }
}