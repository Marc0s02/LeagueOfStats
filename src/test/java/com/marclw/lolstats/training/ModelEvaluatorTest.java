package com.marclw.lolstats.training;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import org.junit.jupiter.api.Test;
import smile.classification.Classifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelEvaluatorTest {

    private final ModelEvaluator evaluator = new ModelEvaluator();

    private FeatureVector row(double goldDiff, Boolean blueWon) {
        FeatureVector vector = new FeatureVector();
        for (String name : FeatureSpec.FEATURE_NAMES) {
            vector.put(name, 0.0);
        }
        vector.put("goldDiffAtMinute", goldDiff);
        vector.setBlueTeamWon(blueWon);
        return vector;
    }

    /**
     * A fake classifier that says "blue wins iff gold diff is positive".
     * Using a stub rather than a trained model keeps these tests about the
     * evaluator's arithmetic, not about whether Smile converged.
     */
    private Classifier<double[]> goldSignClassifier() {
        int goldIndex = FeatureSpec.FEATURE_NAMES.indexOf("goldDiffAtMinute");
        return new Classifier<>() {
            @Override
            public int predict(double[] x) {
                return x[goldIndex] > 0 ? 1 : 0;
            }

            @Override
            public int predict(double[] x, double[] posteriori) {
                double blue = x[goldIndex] > 0 ? 0.9 : 0.1;
                posteriori[0] = 1 - blue;
                posteriori[1] = blue;
                return x[goldIndex] > 0 ? 1 : 0;
            }

            @Override
            public boolean isSoft() {
                return true;
            }

            @Override
            public int numClasses() {
                return 2;
            }
        };
    }

    @Test
    void accuracyIsOneWhenModelIsAlwaysRight() {
        List<FeatureVector> testRows = List.of(
                row(1000, true), row(-1000, false), row(500, true), row(-500, false));
        assertEquals(1.0, evaluator.accuracy(goldSignClassifier(), testRows));
    }

    @Test
    void accuracyIsZeroWhenModelIsAlwaysWrong() {
        List<FeatureVector> testRows = List.of(
                row(1000, false), row(-1000, true));
        assertEquals(0.0, evaluator.accuracy(goldSignClassifier(), testRows));
    }

    @Test
    void accuracyIsHalfOnAnEvenSplit() {
        List<FeatureVector> testRows = List.of(
                row(1000, true), row(-1000, true));
        assertEquals(0.5, evaluator.accuracy(goldSignClassifier(), testRows));
    }

    @Test
    void baselineAccuracyIsTheBlueWinRate() {
        // 3 of 4 rows are blue wins, so "always predict blue" is right 75%.
        List<FeatureVector> testRows = List.of(
                row(1, true), row(2, true), row(3, true), row(4, false));
        assertEquals(0.75, evaluator.baselineAccuracy(testRows));
    }

    @Test
    void aucIsOneForPerfectRanking() {
        List<FeatureVector> testRows = List.of(
                row(1000, true), row(2000, true), row(-1000, false), row(-2000, false));
        assertEquals(1.0, evaluator.areaUnderRocCurve(goldSignClassifier(), testRows), 1e-9);
    }

    @Test
    void aucIsInValidRange() {
        List<FeatureVector> testRows = List.of(
                row(1000, true), row(-1000, true), row(500, false), row(-500, false));
        double auc = evaluator.areaUnderRocCurve(goldSignClassifier(), testRows);
        assertTrue(auc >= 0.0 && auc <= 1.0, "AUC out of range: " + auc);
    }

    @Test
    void emptyTestSetReturnsZeroRatherThanDividingByZero() {
        assertEquals(0.0, evaluator.accuracy(goldSignClassifier(), List.of()));
        assertEquals(0.0, evaluator.baselineAccuracy(List.of()));
        assertEquals(0.0, evaluator.areaUnderRocCurve(goldSignClassifier(), List.of()));
    }

    @Test
    void rejectsUnlabeledTestRows() {
        List<FeatureVector> testRows = new ArrayList<>();
        testRows.add(row(1000, null));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.accuracy(goldSignClassifier(), testRows));
    }
}
