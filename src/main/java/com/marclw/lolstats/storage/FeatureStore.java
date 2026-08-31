package com.marclw.lolstats.storage;

import com.marclw.lolstats.model.FeatureVector;

import java.nio.file.Path;
import java.util.List;

/**
 * Persists/reloads the gold-layer feature table DatasetBuilder produces.
 *
 * A flat CSV (one row per FeatureVector, columns from FeatureSpec.FEATURE_NAMES
 * plus a blueTeamWon label column) is a perfectly legitimate choice here
 * and keeps the project's storage story simple/inspectable; a real
 * database (Postgres/DuckDB) is a reasonable upgrade if the elective wants
 * to see a proper warehouse layer, but isn't required for the pipeline to
 * work correctly.
 */
public class FeatureStore {

    private Path storageLocation;

    public FeatureStore() {
    }

    public FeatureStore(Path storageLocation) {
        this.storageLocation = storageLocation;
    }

    public void writeFeatureTable(List<FeatureVector> rows) {
    }

    public List<FeatureVector> readFeatureTable() {
        return null;
    }

    public Path getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(Path storageLocation) {
        this.storageLocation = storageLocation;
    }
}
