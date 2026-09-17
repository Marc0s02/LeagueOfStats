package com.marclw.lolstats.training;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.prediction.ModelScorer;
import org.junit.jupiter.api.Test;
import smile.classification.Classifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTrainerTest {

    /**
     * Builds a row where gold diff is the only signal - blue ahead on gold
     * means blue won. A model that can't learn this trivially separable
     * relationship is broken.
     */
    private FeatureVector row(double goldDiff, boolean blueWon) {
        FeatureVector vector = new FeatureVector();
        for (String name : FeatureSpec.FEATURE_NAMES) {
            vector.put(name, 0.0);
        }
        vector.put("goldDiffAtMinute", goldDiff);
        vector.setBlueTeamWon(blueWon);
        return vector;
    }

    private List<FeatureVector> separableTrainingSet() {
        List<FeatureVector> rows = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            rows.add(row(i * 100.0, true));    // blue ahead -> blue won
            rows.add(row(i * -100.0, false));  // blue behind -> blue lost
        }
        return rows;
    }

    @Test
    void trainsAModelOnSeparableData() {
        Classifier<double[]> model = new ModelTrainer().trainLogisticRegression(separableTrainingSet());
        assertNotNull(model);
    }

    @Test
    void learnedModelPredictsTheObviousRelationship() {
        Classifier<double[]> model = new ModelTrainer().trainLogisticRegression(separableTrainingSet());

        double blueAheadProbability = ModelScorer.blueWinProbability(model, row(5000, true));
        double blueBehindProbability = ModelScorer.blueWinProbability(model, row(-5000, false));

        assertTrue(blueAheadProbability > 0.5,
                "big gold lead should give blue >50% win probability, got " + blueAheadProbability);
        assertTrue(blueBehindProbability < 0.5,
                "big gold deficit should give blue <50% win probability, got " + blueBehindProbability);
    }

    @Test
    void probabilitiesAreInValidRange() {
        Classifier<double[]> model = new ModelTrainer().trainLogisticRegression(separableTrainingSet());
        double probability = ModelScorer.blueWinProbability(model, row(1234, true));
        assertTrue(probability >= 0.0 && probability <= 1.0,
                "probability out of [0,1]: " + probability);
    }

    @Test
    void rejectsUnlabeledRows() {
        FeatureVector unlabeled = row(100, true);
        unlabeled.setBlueTeamWon(null);

        List<FeatureVector> rows = new ArrayList<>(separableTrainingSet());
        rows.add(unlabeled);

        assertThrows(IllegalArgumentException.class,
                () -> new ModelTrainer().trainLogisticRegression(rows));
    }

    @Test
    void rejectsSingleClassTrainingSet() {
        List<FeatureVector> allBlueWins = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            allBlueWins.add(row(i * 100.0, true));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new ModelTrainer().trainLogisticRegression(allBlueWins));
    }

    @Test
    void featureSpecToArrayPreservesDeclaredOrder() {
        // The whole training/serving-skew guarantee rests on this, so it's
        // worth asserting rather than assuming.
        FeatureVector vector = new FeatureVector();
        for (int i = 0; i < FeatureSpec.FEATURE_NAMES.size(); i++) {
            vector.put(FeatureSpec.FEATURE_NAMES.get(i), i);
        }
        double[] values = FeatureSpec.toArray(vector);

        assertEquals(FeatureSpec.FEATURE_NAMES.size(), values.length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i], "column " + i + " out of order");
        }
    }

    @Test
    void gradientBoostedTreesIsExplicitlyUnimplemented() {
        assertThrows(UnsupportedOperationException.class,
                () -> new ModelTrainer().trainGradientBoostedTrees(separableTrainingSet()));
    }
}