package com.marclw.lolstats.recommendation;

import com.marclw.lolstats.model.Role;
import com.marclw.lolstats.model.Rune;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based RuneRecommender - mirrors HeuristicBuildRecommender's
 * approach and shares its "role is an imprecise proxy for damage type"
 * caveat (see that class's javadoc).
 *
 * NOT YET WIRABLE END-TO-END: there is no ingest path for rune data yet.
 * Data Dragon's runesReforged.json (cdn/{version}/data/en_US/
 * runesReforged.json) hasn't been added to DataDragonClient, and there's
 * no RuneRepository mirroring ChampionRepository/ItemRepository to cache
 * and expose it. This class is written against the List<Rune> shape that
 * repository would produce, so plugging it in later should just mean
 * constructing a real RuneRepository and passing its output here - no
 * change to this class's logic expected.
 */
public class HeuristicRuneRecommender implements RuneRecommender {

    private final List<Rune> allRunes;

    public HeuristicRuneRecommender(List<Rune> allRunes) {
        this.allRunes = allRunes;
    }

    @Override
    public RecommendedRunePage recommend(GameState gameState) {
        boolean myChampionIsMage = gameState.getMyChampion().getRoles() != null
                && gameState.getMyChampion().getRoles().contains(Role.MAGE);

        RecommendedRunePage page = new RecommendedRunePage();
        List<String> notes = new ArrayList<>();

        // TODO: this only picks a keystone based on your own champion's
        // role, completely ignoring the enemy comp - a real version should
        // also react to things like "enemy has a lot of CC" (Guardian/
        // Second Wind-style resolve picks) or "enemy is short-ranged and
        // easy to poke" (Sorcery/Domination picks), not just your own
        // champion's class.
        String keystoneName = myChampionIsMage ? "Arcane Comet" : "Conqueror";
        Optional<Rune> keystone = findRuneByName(keystoneName);
        keystone.ifPresent(page::setKeystone);
        notes.add(keystone.isPresent()
                ? "Keystone picked from your champion's role only - enemy comp isn't factored in yet."
                : "Rune data not loaded - RuneRepository needs to be wired up first (see class javadoc).");

        page.setPlaystyleNotes(notes);
        return page;
    }

    private Optional<Rune> findRuneByName(String name) {
        if (allRunes == null) {
            return Optional.empty();
        }
        return allRunes.stream()
                .filter(rune -> name.equalsIgnoreCase(rune.getName()))
                .findFirst();
    }
}
