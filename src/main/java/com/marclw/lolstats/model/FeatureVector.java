package com.marclw.lolstats.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One ML-ready row (gold layer): a fixed, ordered set of numeric features
 * describing match state at a given minute, plus (for training data only)
 * the known outcome label.
 *
 * Backed by a LinkedHashMap keyed by feature name rather than a fixed set
 * of fields, so FeatureSpec stays the single source of truth for "what
 * features exist and in what order" - both DatasetBuilder (training) and
 * PredictionService (serving) read/write through this same shape, which is
 * what keeps them from drifting apart (see FeatureSpec).
 */
public class FeatureVector {

    private final Map<String, Double> features = new LinkedHashMap<>();

    // Only populated for historical/training rows; null/unused at serving time.
    private Boolean blueTeamWon;

    public FeatureVector() {
    }

    public void put(String featureName, double value) {
        features.put(featureName, value);
    }

    public double get(String featureName) {
        return features.getOrDefault(featureName, 0.0);
    }

    public Map<String, Double> getFeatures() {
        return features;
    }

    public Boolean getBlueTeamWon() {
        return blueTeamWon;
    }

    public void setBlueTeamWon(Boolean blueTeamWon) {
        this.blueTeamWon = blueTeamWon;
    }
}
