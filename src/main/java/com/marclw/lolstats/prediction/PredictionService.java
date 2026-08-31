package com.marclw.lolstats.prediction;

import com.marclw.lolstats.storage.ModelRegistry;

/**
 * Online path: what actually runs when a user asks for a prediction from
 * PredictionController. Deliberately thin - all the real logic
 * (feature computation) lives in features.FeatureExtractor and is shared
 * with the offline training path; this class just wires fetch -> extract
 * -> load model -> score together.
 *
 * TODO once ingest clients exist: this needs a RiotMatchClient (recently-
 * finished games) and/or RiotSpectatorClient (in-progress games) injected
 * alongside ModelRegistry, plus the same FeatureExtractor/TeamAggregator
 * instances DatasetBuilder uses (construct once in App.java and pass both
 * to DatasetBuilder and PredictionService, don't build two copies).
 */
public class PredictionService {

    private final ModelRegistry modelRegistry;

    public PredictionService(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    /**
     * @param matchId a completed match ID, or an active-game ID for a live
     *                game - resolving which API to hit for which is left
     *                to this method once the ingest clients are wired in
     */
    public WinProbabilityResult predict(String matchId, int minute) {
        return null;
    }
}
