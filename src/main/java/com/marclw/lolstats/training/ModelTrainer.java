package com.marclw.lolstats.training;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import smile.classification.Classifier;
import smile.classification.LogisticRegression;

import java.util.List;

/**
 * Offline path, step 2: fits a win-probability classifier on the
 * FeatureVector table DatasetBuilder produces.
 *
 * Uses Smile (com.github.haifengl:smile-core, see pom.xml) rather than
 * hand-rolling gradient descent. Feature column order comes from
 * FeatureSpec.toArray, NOT from FeatureVector's map iteration order, so
 * the model is always fitted on the same column layout the serving path
 * will later score against.
 *
 * Labels: 1 = blue team won, 0 = red team won.
 */
public class ModelTrainer {

    /**
     * @param trainingRows FeatureVectors with blueTeamWon set (from
     *                      DatasetBuilder), already split so this only
     *                      receives the training portion - keep a held-out
     *                      test portion for ModelEvaluator, don't train on
     *                      everything.
     */
    public Classifier<double[]> trainLogisticRegression(List<FeatureVector> trainingRows) {
        double[][] x = toDesignMatrix(trainingRows);
        int[] y = toLabels(trainingRows);
        requireBothClassesPresent(y);
        return LogisticRegression.fit(x, y, 0.3, 1E-5, 500);
    }

    /**
     * NOT IMPLEMENTED YET. Smile's GradientTreeBoost takes a
     * Formula + DataFrame rather than the plain double[][]/int[] pair
     * LogisticRegression accepts, so this needs a DataFrame built with
     * named columns matching FeatureSpec.FEATURE_NAMES plus a label
     * column. Left as a clear stub rather than a guess - get logistic
     * regression working and evaluated first, then this becomes a
     * "does a non-linear model beat the linear baseline" comparison
     * worth having in the report.
     */
    public Classifier<double[]> trainGradientBoostedTrees(List<FeatureVector> trainingRows) {
        throw new UnsupportedOperationException(
                "Gradient-boosted trees not implemented yet - use trainLogisticRegression");
    }

    private double[][] toDesignMatrix(List<FeatureVector> rows) {
        double[][] x = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            x[i] = FeatureSpec.toArray(rows.get(i));
        }
        return x;
    }

    private int[] toLabels(List<FeatureVector> rows) {
        int[] y = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            Boolean blueWon = rows.get(i).getBlueTeamWon();
            if (blueWon == null) {
                throw new IllegalArgumentException(
                        "Training row " + i + " has no blueTeamWon label - "
                                + "unlabeled rows can't be trained on");
            }
            y[i] = blueWon ? 1 : 0;
        }
        return y;
    }

    /**
     * A training set where every game went the same way can't teach a
     * classifier anything, and Smile's label remapping would also
     * produce a single-class model whose posteriori[] indexing differs
     * from what ModelScorer assumes. Fail loudly instead.
     */
    private void requireBothClassesPresent(int[] y) {
        boolean sawBlueWin = false;
        boolean sawRedWin = false;
        for (int label : y) {
            if (label == 1) sawBlueWin = true; else sawRedWin = true;
        }
        if (!sawBlueWin || !sawRedWin) {
            throw new IllegalArgumentException(
                    "Training set contains only one outcome class - need both "
                            + "blue wins and red wins to fit a classifier");
        }
    }
}