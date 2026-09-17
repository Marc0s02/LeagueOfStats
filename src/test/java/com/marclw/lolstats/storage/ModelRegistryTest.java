package com.marclw.lolstats.storage;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.prediction.ModelScorer;
import com.marclw.lolstats.training.ModelTrainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRegistryTest {

    private FeatureVector row(double goldDiff, boolean blueWon) {
        FeatureVector vector = new FeatureVector();
        for (String name : FeatureSpec.FEATURE_NAMES) {
            vector.put(name, 0.0);
        }
        vector.put("goldDiffAtMinute", goldDiff);
        vector.setBlueTeamWon(blueWon);
        return vector;
    }

    private smile.classification.SoftClassifier<double[]> trainedModel() {
        List<FeatureVector> rows = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            rows.add(row(i * 100.0, true));
            rows.add(row(i * -100.0, false));
        }
        return new ModelTrainer().trainLogisticRegression(rows);
    }

    @Test
    void savedModelCanBeLoadedBack(@TempDir Path tempDir) {
        ModelRegistry registry = new ModelRegistry(tempDir.resolve("model"));
        registry.saveModel(trainedModel(), FeatureSpec.VERSION);

        assertNotNull(registry.loadModel());
    }

    @Test
    void loadedModelScoresIdenticallyToTheSavedOne(@TempDir Path tempDir) {
        // Round-tripping through serialization must not change predictions -
        // otherwise the evaluation numbers reported from training wouldn't
        // describe the model actually being served.
        ModelRegistry registry = new ModelRegistry(tempDir.resolve("model"));
        var original = trainedModel();
        registry.saveModel(original, FeatureSpec.VERSION);
        var loaded = registry.loadModel();

        FeatureVector probe = row(3333, true);
        assertEquals(ModelScorer.blueWinProbability(original, probe),
                ModelScorer.blueWinProbability(loaded, probe), 1e-12);
    }

    @Test
    void refusesToLoadAModelTrainedAgainstADifferentFeatureSpec(@TempDir Path tempDir) {
        ModelRegistry registry = new ModelRegistry(tempDir.resolve("model"));
        registry.saveModel(trainedModel(), "some-older-version");

        IllegalStateException error = assertThrows(IllegalStateException.class, registry::loadModel);
        assertTrue(error.getMessage().contains("some-older-version"),
                "error should name the mismatched version so the cause is obvious");
    }

    @Test
    void failsClearlyWhenNoModelHasBeenSaved(@TempDir Path tempDir) {
        ModelRegistry registry = new ModelRegistry(tempDir.resolve("model"));
        IllegalStateException error = assertThrows(IllegalStateException.class, registry::loadModel);
        assertTrue(error.getMessage().toLowerCase().contains("train"),
                "error should tell the user to train a model first");
    }

    @Test
    void writesMetadataAlongsideTheModel(@TempDir Path tempDir) throws IOException {
        Path modelDirectory = tempDir.resolve("model");
        new ModelRegistry(modelDirectory).saveModel(trainedModel(), FeatureSpec.VERSION);

        assertTrue(Files.exists(modelDirectory.resolve("model.ser")));
        assertTrue(Files.exists(modelDirectory.resolve("model.properties")));
        assertTrue(Files.readString(modelDirectory.resolve("model.properties"))
                .contains(FeatureSpec.VERSION));
    }

    @Test
    void savingTwiceOverwritesRatherThanFailing(@TempDir Path tempDir) {
        ModelRegistry registry = new ModelRegistry(tempDir.resolve("model"));
        registry.saveModel(trainedModel(), FeatureSpec.VERSION);
        registry.saveModel(trainedModel(), FeatureSpec.VERSION);
        assertNotNull(registry.loadModel());
    }
}