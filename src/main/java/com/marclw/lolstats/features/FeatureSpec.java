package com.marclw.lolstats.features;

import java.util.List;

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
