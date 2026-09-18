package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.GameMode;
import com.marclw.lolstats.model.Item;

/**
 * Central place for deciding whether an item is selectable by the calculator.
 * The UI should never have to understand Riot/CommunityDragon item flags.
 */
public final class ItemRules {

    private static final String SUMMONERS_RIFT_MAP_ID = "11";

    private ItemRules() {
    }

    public static boolean isSelectable(Item item, GameMode mode, Champion champion) {
        if (item == null || mode != GameMode.SUMMONERS_RIFT) {
            return false;
        }

        if (!item.isInStore()
                || !item.isPurchasable()
                || !item.isDisplayInItemSets()
                || item.isHiddenFromAll()
                || item.getCost() <= 0
                || !item.isAvailableOnMap(SUMMONERS_RIFT_MAP_ID)) {
            return false;
        }

        String requiredChampion = normalize(item.getRequiredChampion());
        if (!requiredChampion.isEmpty()) {
            return champion != null && requiredChampion.equalsIgnoreCase(normalize(champion.getName()));
        }

        // requiredAlly items need a team-state rule that this calculator does
        // not model yet, so do not expose them as freely selectable items.
        if (!normalize(item.getRequiredAlly()).isEmpty()) {
            return false;
        }

        return true;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
