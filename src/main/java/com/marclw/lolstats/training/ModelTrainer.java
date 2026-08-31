package com.marclw.lolstats.training;

import com.marclw.lolstats.model.FeatureVector;

import java.util.List;

/**
 * Offline path, step 2: fits a win-probability classifier on the
 * FeatureVector table DatasetBuilder produces.
 *
 * Intended to use Smile (smile.classification.LogisticRegression or
 * smile.classification.GradientTreeBoost - see pom.xml, com.github.haifengl:smile-core)
 * rather than hand-rolling gradient descent. The exact call shape depends
 * on Smile's DataFrame/Formula API - left as a TODO rather than guessed at
 * here, since getting the feature-column <-> label wiring wrong silently
 * would be worse than a clear stub.
 *
 * Returns whatever type ends up being the trained model - keep that type
 * consistent with what ModelRegistry.saveModel()/loadModel() expect, and
 * with what PredictionService needs to call .predict() on.
 */
public class ModelTrainer {

    /**
     * @param trainingRows FeatureVectors with blueTeamWon set (from
     *                      DatasetBuilder), already split so this only
     *                      receives the training portion - keep a held-out
     *                      test portion for ModelEvaluator, don't train on
     *                      everything.
     */
    public Object trainLogisticRegression(List<FeatureVector> trainingRows) {
        return null;
    }

    public Object trainGradientBoostedTrees(List<FeatureVector> trainingRows) {
        return null;
    }
}
