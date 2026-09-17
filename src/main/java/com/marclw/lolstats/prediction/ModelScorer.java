package com.marclw.lolstats.prediction;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import smile.classification.Classifier;

/**
 * Turns a trained classifier + a FeatureVector into blue's win
 * probability. Extracted into its own class because BOTH ModelEvaluator
 * (offline, measuring quality) and PredictionService (online, serving a
 * user request) need exactly this, and having two copies of "which index
 * of posteriori[] is the blue-won class" is precisely the kind of
 * duplication that causes training/serving skew.
 */
public final class ModelScorer {

    private ModelScorer() {
    }

    /**
     * Smile remaps labels to contiguous indices internally, sorted - so
     * with labels {0, 1} (0 = red won, 1 = blue won), posteriori[1] is
     * P(blue won). This holds as long as both classes appear in the
     * training data, which ModelTrainer enforces.
     */
    public static double blueWinProbability(Classifier<double[]> model, FeatureVector row) {
        double[] posteriori = new double[2];
        model.predict(FeatureSpec.toArray(row), posteriori);
        return posteriori[1];
    }
}