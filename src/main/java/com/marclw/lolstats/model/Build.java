package com.marclw.lolstats.model;

import java.util.ArrayList;
import java.util.List;

public class Build {

    private List<Item> items = new ArrayList<>();

    public void addItem(Item item) {
        items.add(item);
    }

    /**
     * Sums this build's items' stat bonuses via repeated Stats.add(). An
     * empty build correctly returns a zeroed Stats rather than null, so
     * StatCalculator.calculateFinalStats() can always call
     * champion.getStatsAtLevel(level).add(build.getTotalStats()) without a
     * null check, even before any items are selected.
     */
    public Stats getTotalStats() {
        Stats total = new Stats(0, 0, 0, 0, 0, 0, 0);
        for (Item item : items) {
            // Defensive: an item deserialized from Community Dragon with no
            // stat bonuses (e.g. a pure-passive item) may have a null
            // statBonuses field rather than a zeroed Stats object.
            if (item.getStatBonuses() != null) {
                total = total.add(item.getStatBonuses());
            }
        }
        return total;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
