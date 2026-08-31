package com.marclw.lolstats.storage;

import java.nio.file.Path;

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
 * and refusing to load on a mismatch (or at least warning loudly) turns
 * that into a caught error instead of a silent one.
 */
public class ModelRegistry {

    private Path modelDirectory;

    public ModelRegistry() {
    }

    public ModelRegistry(Path modelDirectory) {
        this.modelDirectory = modelDirectory;
    }

    /**
     * Persists the trained model plus FeatureSpec.VERSION as metadata
     * alongside it.
     */
    public void saveModel(Object model, String featureSpecVersion) {
    }

    /**
     * Loads the most recently saved model. Should check the stored
     * featureSpecVersion against FeatureSpec.VERSION and fail loudly (or
     * at minimum log a clear warning) on mismatch, rather than returning
     * a model that will silently misinterpret its input.
     */
    public Object loadModel() {
        return null;
    }

    public Path getModelDirectory() {
        return modelDirectory;
    }

    public void setModelDirectory(Path modelDirectory) {
        this.modelDirectory = modelDirectory;
    }
}
