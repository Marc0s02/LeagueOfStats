package com.marclw.lolstats.recommendation;

import com.marclw.lolstats.model.Item;

import java.util.List;

/**
 * Output of a BuildRecommender: an ordered list of items with a short,
 * human-readable reason for each (shown directly in the UI, e.g. "Enemy
 * team has 3+ magic damage threats"), plus an overall summary line.
 *
 * Order matters and is meant to reflect suggested build order, not just
 * an unordered "good items" set - HeuristicBuildRecommender should build
 * this list core-item-first.
 */
public class RecommendedBuild {

    private List<ItemRecommendation> items;
    private String summary;

    public RecommendedBuild() {
    }

    public RecommendedBuild(List<ItemRecommendation> items, String summary) {
        this.items = items;
        this.summary = summary;
    }

    public List<ItemRecommendation> getItems() {
        return items;
    }

    public void setItems(List<ItemRecommendation> items) {
        this.items = items;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * winRatePercent/gamesSampled are only populated when
     * RecommendationSettings.useRealMatchData is on and
     * MatchupStatsProvider actually had data for this item+matchup - null
     * otherwise (heuristic-only mode, or insufficient sample size). The UI
     * should treat null as "no stats available", not 0%.
     */
    public static class ItemRecommendation {
        private Item item;
        private String reasoning;
        private Double winRatePercent;
        private Integer gamesSampled;

        public ItemRecommendation() {
        }

        public ItemRecommendation(Item item, String reasoning) {
            this.item = item;
            this.reasoning = reasoning;
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public Double getWinRatePercent() {
            return winRatePercent;
        }

        public void setWinRatePercent(Double winRatePercent) {
            this.winRatePercent = winRatePercent;
        }

        public Integer getGamesSampled() {
            return gamesSampled;
        }

        public void setGamesSampled(Integer gamesSampled) {
            this.gamesSampled = gamesSampled;
        }
    }
}
