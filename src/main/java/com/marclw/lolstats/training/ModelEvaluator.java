package com.marclw.lolstats.training;

import com.marclw.lolstats.model.FeatureVector;

import java.util.List;

/**
 * Offline path, step 3: measures a trained model's quality against a
 * held-out test set (rows DatasetBuilder produced but ModelTrainer never
 * saw). This is the piece that turns "I trained a model" into an actual,
 * defensible claim - worth keeping the numbers (and ideally a couple of
 * baseline comparisons, e.g. accuracy vs. always-predicting-blue-wins) for
 * the project report.
 */
public class ModelEvaluator {

    /**
     * @param model     whatever ModelTrainer produced
     * @param testRows  held-out FeatureVectors with blueTeamWon set,
     *                  not used during training
     */
    public double accuracy(Object model, List<FeatureVector> testRows) {
        return 0;
    }

    /**
     * Area under the ROC curve - more informative than raw accuracy for a
     * roughly-balanced win/loss classification problem.
     */
    public double areaUnderRocCurve(Object model, List<FeatureVector> testRows) {
        return 0;
    }

    /**
     * Accuracy of a trivial baseline (e.g. "blue side always wins") on the
     * same test set, so the real model's accuracy can be reported as an
     * improvement over chance/baseline rather than a bare number.
     */
    public double baselineAccuracy(List<FeatureVector> testRows) {
        return 0;
    }
}
