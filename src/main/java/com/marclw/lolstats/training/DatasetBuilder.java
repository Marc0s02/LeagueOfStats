package com.marclw.lolstats.training;

import com.marclw.lolstats.features.FeatureExtractor;
import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;

import java.util.List;
import java.util.Map;

/**
 * Offline path, step 1: turns a batch of historical matches into a flat
 * table of FeatureVectors ready for ModelTrainer - the "gold layer" this
 * project's data pipeline produces.
 *
 * Source matches can come from HistoricalDatasetLoader (bulk file) and/or
 * RiotMatchClient (API pulls) - this class doesn't care which, it just
 * needs a MatchRecord + matching MatchTimeline per game.
 *
 * Worth persisting the resulting table to disk (via FeatureStore) even
 * before training - it's a genuinely useful standalone artifact, and lets
 * ModelTrainer be re-run/tuned without re-fetching or re-extracting.
 */
public class DatasetBuilder {

    private final FeatureExtractor featureExtractor;

    public DatasetBuilder(FeatureExtractor featureExtractor) {
        this.featureExtractor = featureExtractor;
    }

    /**
     * @param matches   historical matches with known outcomes
     * @param timelines matchId -> that match's timeline
     * @param minute    the "as of minute" to extract features at for every
     *                  row - e.g. 15, if the model is meant to predict
     *                  outcome from 15-minute state. Could later be
     *                  extended to sample multiple minutes per match
     *                  instead of a single fixed one.
     */
    public List<FeatureVector> buildTrainingSet(List<MatchRecord> matches,
                                                Map<String, MatchTimeline> timelines,
                                                int minute) {
        return null;
    }
}
