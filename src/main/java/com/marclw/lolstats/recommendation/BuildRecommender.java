package com.marclw.lolstats.recommendation;

/**
 * GameState -> RecommendedBuild. HeuristicBuildRecommender is the base
 * implementation; StatsAugmentedBuildRecommender wraps it to layer real
 * match-data win rates on top when RecommendationSettings.useRealMatchData
 * is on. Kept as an interface (rather than a toggle flag inside one class)
 * so the two concerns - domain heuristics vs. statistical aggregation -
 * stay in separate, independently testable classes; a future ML-based
 * implementation would be a third implementation of this same interface.
 */
public interface BuildRecommender {

    RecommendedBuild recommend(GameState gameState);
}
