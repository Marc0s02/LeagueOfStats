package com.marclw.lolstats.features;

import java.util.List;
import com.marclw.lolstats.model.FeatureVector;

/**
 * The single source of truth for "what features exist, and in what order".
 *
 * Both DatasetBuilder (training) and PredictionService (serving) build
 * FeatureVectors via FeatureExtractor, and both need to agree exactly on
 * feature names/order for the model to make sense of its input. Rather
 * than hardcoding a feature list in two places (and risking them silently
 * drifting apart - "training/serving skew"), everything reads this class.
 *
 * ModelRegistry should persist a FeatureSpec's version/feature list
 * alongside every saved model, so loading an old model against a newer
 * FeatureSpec can be detected instead of silently producing garbage
 * predictions.
 */
public final class FeatureSpec {

    public static final String VERSION = "v1";

    /**
     * Flattens a FeatureVector into a plain double[] in FEATURE_NAMES order -
     * the shape Smile's classifiers expect. Lives here rather than on
     * FeatureVector itself so the ordering logic sits with the ordering
     * definition; both training and serving paths must use this same method,
     * or the model gets columns in a different order than it was fitted on.
     */
    public static double[] toArray(FeatureVector vector) {
        double[] values = new double[FEATURE_NAMES.size()];
        for (int i = 0; i < FEATURE_NAMES.size(); i++) {
            values[i] = vector.get(FEATURE_NAMES.get(i));
        }
        return values;
    }

    // Ordered on purpose - this is the exact column order FeatureVector
    // values are written in and the model is trained on.
    public static final List<String> FEATURE_NAMES = List.of(
            "goldDiffAtMinute",
            "xpDiffAtMinute",
            "killDiffAtMinute",
            "dragonDiffAtMinute",
            "heraldDiffAtMinute",
            "towerDiffAtMinute",
            "blueTeamAvgChampionWinRate",
            "redTeamAvgChampionWinRate",
            "blueTeamCompositionEngageScore",
            "redTeamCompositionEngageScore"
    );

    private FeatureSpec() {
        // no instances - static holder only
    }
}
