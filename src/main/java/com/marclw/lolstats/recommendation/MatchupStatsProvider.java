package com.marclw.lolstats.recommendation;

/**
 * Queries pre-aggregated real-match win rates, used only when
 * RecommendationSettings.useRealMatchData is on. Implemented by
 * com.marclw.lolstats.stats.MatchupStatsStore, which loads a table built
 * offline by MatchupStatsBuilder - this interface is what
 * StatsAugmentedBuildRecommender depends on, kept separate from the
 * concrete store so the recommendation package doesn't need to know
 * anything about how/where the aggregated data is persisted.
 *
 * All methods return null (not 0.0/empty) when there isn't enough sample
 * data to be meaningful - callers must treat "no data" and "0% win rate"
 * as genuinely different things, not collapse them.
 */
public interface MatchupStatsProvider {

    /**
     * Win rate for championId when laned/played against opposingChampionId,
     * as a percentage (0-100). Null if there's no recorded matchup data
     * for this pair.
     */
    Double winRateForMatchup(int championId, int opposingChampionId);

    /**
     * The most commonly built items for championId specifically when
     * facing opposingChampionId, most-picked first, capped at topN. Empty
     * (not null) if there's no data - an empty list is easy for callers to
     * iterate over without a null check, unlike winRateForMatchup where
     * null vs. a real percentage is a meaningful distinction to preserve.
     */
    java.util.List<Integer> mostPopularItemIdsForMatchup(int championId, int opposingChampionId, int topN);

    /**
     * Win rate for championId when it was built with itemId as part of
     * its final build, across all matchups (not matchup-specific) - used
     * to annotate individual ItemRecommendation.winRatePercent values.
     * Null if there's no recorded data for this champion+item pair.
     */
    Double winRateForChampionWithItem(int championId, int itemId);

    /**
     * How many games the winRateForChampionWithItem figure above is based
     * on - callers (or the UI) may want to visually de-emphasize a
     * headline win rate backed by a tiny sample.
     */
    Integer gamesSampledForChampionWithItem(int championId, int itemId);
}
