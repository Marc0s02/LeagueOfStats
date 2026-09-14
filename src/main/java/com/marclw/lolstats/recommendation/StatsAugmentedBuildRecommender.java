package com.marclw.lolstats.recommendation;

/**
 * Wraps a base BuildRecommender (in practice, HeuristicBuildRecommender)
 * and layers real match-data win rates on top, but only when
 * RecommendationSettings.useRealMatchData is true - otherwise it just
 * passes the delegate's recommendation through unchanged. This is a
 * decorator specifically so the heuristic logic and the "should stats be
 * shown" toggle stay decoupled: HeuristicBuildRecommender has no idea
 * this class exists, and doesn't need to.
 *
 * Deliberately ANNOTATES the delegate's recommendations with win-rate
 * data rather than replacing them outright with "whatever wins most
 * statistically" - the ask was for a toggle that adds real-data context
 * to the rule-based picks, not a second recommendation engine that
 * overrides the first. A future ML-based recommender would more
 * naturally be a third BuildRecommender implementation, not another
 * decorator layered here.
 */
public class StatsAugmentedBuildRecommender implements BuildRecommender {

    private final BuildRecommender delegate;
    private final MatchupStatsProvider statsProvider;
    private final RecommendationSettings settings;

    public StatsAugmentedBuildRecommender(BuildRecommender delegate,
                                          MatchupStatsProvider statsProvider,
                                          RecommendationSettings settings) {
        this.delegate = delegate;
        this.statsProvider = statsProvider;
        this.settings = settings;
    }

    @Override
    public RecommendedBuild recommend(GameState gameState) {
        RecommendedBuild build = delegate.recommend(gameState);

        if (!settings.isUseRealMatchData() || build.getItems() == null) {
            return build;
        }

        int myChampionId = gameState.getMyChampion().getId();
        for (RecommendedBuild.ItemRecommendation rec : build.getItems()) {
            int itemId = rec.getItem().getId();
            Double winRate = statsProvider.winRateForChampionWithItem(myChampionId, itemId);
            Integer games = statsProvider.gamesSampledForChampionWithItem(myChampionId, itemId);
            rec.setWinRatePercent(winRate);
            rec.setGamesSampled(games);
        }

        // TODO: beyond annotating the heuristic picks, consider also
        // adding any high-win-rate item from
        // statsProvider.mostPopularItemIdsForMatchup(...) that the
        // heuristic layer missed entirely, clearly labeled as
        // stats-sourced (e.g. reasoning = "Popular and effective in this
        // exact matchup") rather than blending it in indistinguishably
        // from the rule-based picks.

        return build;
    }
}
