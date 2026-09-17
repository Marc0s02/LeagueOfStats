package com.marclw.lolstats.storage;

import com.marclw.lolstats.features.FeatureSpec;
import smile.classification.Classifier;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Saves/loads a trained model, together with the FeatureSpec.VERSION it
 * was trained against.
 *
 * The FeatureSpec version check on load is the important part here, not
 * just a nice-to-have: if FeatureSpec.FEATURE_NAMES ever changes (a
 * feature added/removed/reordered) after a model was trained, loading
 * that old model and scoring new-shape FeatureVectors against it would
 * silently produce meaningless predictions - same feature *position*,
 * different feature *meaning*. Storing the version alongside the model
 * and refusing to load on a mismatch turns that into a caught error
 * instead of a silent one.
 *
 * Uses Java serialization since Smile's classifiers implement
 * Serializable. That does mean a saved model can fail to load after a
 * Smile version bump - acceptable for a project of this size, and the
 * metadata file makes it obvious what was saved when.
 */
public class ModelRegistry {

    private static final String MODEL_FILE = "model.ser";
    private static final String METADATA_FILE = "model.properties";
    private static final String VERSION_KEY = "featureSpecVersion";
    private static final String SAVED_AT_KEY = "savedAt";

    private Path modelDirectory;

    public ModelRegistry() {
    }

    public ModelRegistry(Path modelDirectory) {
        this.modelDirectory = modelDirectory;
    }

    /**
     * Persists the trained model plus FeatureSpec.VERSION as metadata
     * alongside it. Overwrites any previously saved model.
     */
    public void saveModel(Classifier<double[]> model, String featureSpecVersion) {
        try {
            Files.createDirectories(modelDirectory);
            try (ObjectOutputStream out = new ObjectOutputStream(
                    Files.newOutputStream(modelDirectory.resolve(MODEL_FILE)))) {
                out.writeObject(model);
            }
            Properties metadata = new Properties();
            metadata.setProperty(VERSION_KEY, featureSpecVersion);
            metadata.setProperty(SAVED_AT_KEY, java.time.Instant.now().toString());
            try (var writer = Files.newBufferedWriter(modelDirectory.resolve(METADATA_FILE))) {
                metadata.store(writer, "Trained model metadata - do not edit by hand");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save model to " + modelDirectory, e);
        }
    }

    /**
     * Loads the most recently saved model, refusing to return one whose
     * stored featureSpecVersion doesn't match the current
     * FeatureSpec.VERSION.
     */
    @SuppressWarnings("unchecked")
    public Classifier<double[]> loadModel() {
        Path metadataPath = modelDirectory.resolve(METADATA_FILE);
        Path modelPath = modelDirectory.resolve(MODEL_FILE);
        if (!Files.exists(modelPath)) {
            throw new IllegalStateException(
                    "No saved model at " + modelPath + " - train one first");
        }

        Properties metadata = new Properties();
        try (var reader = Files.newBufferedReader(metadataPath)) {
            metadata.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read model metadata at " + metadataPath, e);
        }

        String savedVersion = metadata.getProperty(VERSION_KEY);
        if (!FeatureSpec.VERSION.equals(savedVersion)) {
            throw new IllegalStateException(
                    "Model was trained against FeatureSpec " + savedVersion
                            + " but current FeatureSpec is " + FeatureSpec.VERSION
                            + " - retrain before using it, or its predictions will be meaningless");
        }

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(modelPath))) {
            return (Classifier<double[]>) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read model at " + modelPath, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Saved model class not on the classpath - likely a Smile version change "
                            + "since it was saved; retrain", e);
        }
    }

    public Path getModelDirectory() {
        return modelDirectory;
    }

    public void setModelDirectory(Path modelDirectory) {
        this.modelDirectory = modelDirectory;
    }
}