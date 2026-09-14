package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.recommendation.GameState;
import com.marclw.lolstats.recommendation.TeamComposition;

import java.util.List;

/**
 * Builds a GameState purely from champion picks the user selects in the
 * UI - no Riot API dependency at all, so this works without an API key
 * and without an active/live game. The advisor controller calls the
 * setters below as the user picks champions in each dropdown/selector,
 * then getGameState() whenever it needs the current snapshot (e.g. every
 * time a selection changes, to refresh recommendations live).
 */
public class ManualGameStateProvider implements GameStateProvider {

    private Champion myChampion;
    private List<Champion> allyChampions;
    private List<Champion> enemyChampions;

    public void setMyChampion(Champion myChampion) {
        this.myChampion = myChampion;
    }

    public void setAllyChampions(List<Champion> allyChampions) {
        this.allyChampions = allyChampions;
    }

    public void setEnemyChampions(List<Champion> enemyChampions) {
        this.enemyChampions = enemyChampions;
    }

    @Override
    public GameState getGameState() {
        if (myChampion == null) {
            return null; // not enough selected yet to build a meaningful GameState
        }
        return new GameState(
                myChampion,
                new TeamComposition(allyChampions),
                new TeamComposition(enemyChampions),
                GameState.Source.MANUAL);
    }
}
