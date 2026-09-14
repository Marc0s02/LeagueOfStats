package com.marclw.lolstats.recommendation;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Role;

import java.util.List;

/**
 * The champions on one side (either the ally or enemy team) for a given
 * GameState. Deliberately just a thin wrapper around List<Champion>
 * rather than a full 5-slot roster with assigned lanes - Riot's APIs
 * (both Spectator-V5 and Match-V5) don't reliably expose lane assignment
 * either, and the heuristic layer only needs "which champions are on this
 * side", not who's in which lane.
 *
 * May legitimately hold fewer than 5 champions - e.g. a manually-built
 * comp while the user is still picking, or a live game fetched before
 * all 10 picks are locked in. Callers should not assume size() == 5.
 */
public class TeamComposition {

    private List<Champion> champions;

    public TeamComposition() {
    }

    public TeamComposition(List<Champion> champions) {
        this.champions = champions;
    }

    public List<Champion> getChampions() {
        return champions;
    }

    public void setChampions(List<Champion> champions) {
        this.champions = champions;
    }

    /**
     * Convenience for heuristics that key off broad archetypes (e.g. "how
     * many burst mages does the enemy have") rather than needing the full
     * Champion object. Counts EVERY role a champion has (Champion.roles is
     * a list - e.g. Champion.roles might list a champion as both FIGHTER
     * and TANK), so totals across all roles can exceed champions.size().
     */
    public long countByRole(Role role) {
        if (champions == null) {
            return 0;
        }
        return champions.stream()
                .filter(c -> c.getRoles() != null && c.getRoles().contains(role))
                .count();
    }
}
