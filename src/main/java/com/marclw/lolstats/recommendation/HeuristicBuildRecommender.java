package com.marclw.lolstats.recommendation;

import com.marclw.lolstats.ingest.ItemRepository;
import com.marclw.lolstats.model.Item;
import com.marclw.lolstats.model.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based BuildRecommender - the base recommendation mode (no live
 * match-data dependency, works offline as long as champion/item data is
 * cached). StatsAugmentedBuildRecommender wraps this to add real win-rate
 * annotations on top when the user has that toggle on.
 *
 * IMPORTANT CAVEAT: the specific items/thresholds below are a starting
 * point, not a verified-optimal answer. Two things in particular are
 * approximations worth revisiting with your own LoL knowledge:
 *
 *   1. "Magic vs physical threat" is inferred from Champion.roles
 *      (MAGE -> magic, everything else -> physical), since neither
 *      Champion nor Item currently stores a real damage-type tag. This
 *      misclassifies plenty of real champions (e.g. AP-scaling
 *      Fighters/Assassins like Rumble or Diana would be counted as
 *      physical threats here) - a genuine limitation, not a bug, until a
 *      proper damage-type field gets added to Champion.
 *   2. Item names below (e.g. "Mercury's Treads") are looked up BY NAME
 *      from the live ItemRepository data rather than hardcoded IDs, so
 *      they'll resolve correctly even if IDs change - but item
 *      availability/names/stats do shift between patches, so verify
 *      these choices still make sense on whatever patch is current when
 *      you're actually using this.
 */
public class HeuristicBuildRecommender implements BuildRecommender {

    private final ItemRepository itemRepository;

    public HeuristicBuildRecommender(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public RecommendedBuild recommend(GameState gameState) {
        TeamComposition enemies = gameState.getEnemies();
        long magicThreats = enemies.countByRole(Role.MAGE);
        long physicalThreats = (enemies.getChampions() == null ? 0 : enemies.getChampions().size()) - magicThreats;

        List<RecommendedBuild.ItemRecommendation> items = new ArrayList<>();

        // TODO: this is the part most worth expanding first - right now
        // it only reacts to the magic/physical split. A more complete
        // version should also consider: burst vs. sustained damage (armor
        // vs. HP-based defenses respond differently), whether the enemy
        // has heavy CC (Mercury's Treads / QSS-line items), and your own
        // champion's role (a marksman itemizes very differently from a
        // tank even against the same enemy comp).
        if (magicThreats >= physicalThreats && magicThreats > 0) {
            findItemByName("Spirit Visage").ifPresent(item ->
                    items.add(new RecommendedBuild.ItemRecommendation(item,
                            "Enemy team leans magic damage (" + magicThreats + " magic threat(s)) - "
                                    + "prioritizing magic resist.")));
        } else if (physicalThreats > 0) {
            findItemByName("Thornmail").ifPresent(item ->
                    items.add(new RecommendedBuild.ItemRecommendation(item,
                            "Enemy team leans physical damage (" + physicalThreats + " physical threat(s)) - "
                                    + "prioritizing armor.")));
        }

        String summary = items.isEmpty()
                ? "Not enough enemy composition data to make a confident recommendation yet."
                : "Base build shaped around the enemy team's damage-type split.";

        return new RecommendedBuild(items, summary);
    }

    private Optional<Item> findItemByName(String name) {
        return itemRepository.getItems().stream()
                .filter(item -> name.equalsIgnoreCase(item.getName()))
                .findFirst();
    }
}
