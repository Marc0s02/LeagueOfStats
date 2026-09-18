package com.marclw.lolstats.recommendation;

/**
 * Holds the single user-facing toggle: whether recommendations should be
 * annotated/reordered using real match-data win rates
 * (StatsAugmentedBuildRecommender + MatchupStatsProvider) on top of the
 * base heuristic recommendation, or left as pure heuristics.
 *
 * Deliberately a small standalone holder rather than a boolean field on
 * ViewManager or a controller, so it can be passed to both
 * StatsAugmentedBuildRecommender and the advisor UI without those two
 * needing a reference to each other.
 */
public class RecommendationSettings {

    private boolean useRealMatchData = false;

    public boolean isUseRealMatchData() {
        return useRealMatchData;
    }

    public void setUseRealMatchData(boolean useRealMatchData) {
        this.useRealMatchData = useRealMatchData;
    }
}
