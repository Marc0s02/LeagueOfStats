package com.marclw.lolstats.ingest;

import com.marclw.lolstats.recommendation.GameState;

/**
 * Produces a GameState for the recommendation layer to consume, from
 * either source the user chose: LiveGameStateProvider (Spectator-V5) or
 * ManualGameStateProvider (UI-selected champions, no API). Neither
 * BuildRecommender nor RuneRecommender depend on this interface directly
 * - they only see the resulting GameState - so this exists purely to let
 * the advisor UI/controller swap sources without an if/else on which
 * provider it's holding.
 */
public interface GameStateProvider {

    /**
     * Returns null if a GameState can't currently be produced - e.g. the
     * live provider found no active game for this summoner, or the manual
     * provider hasn't had all its selections set yet. Callers should
     * treat null as "not ready", not as an error to propagate.
     */
    GameState getGameState();
}
