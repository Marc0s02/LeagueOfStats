package com.marclw.lolstats.recommendation;

import com.marclw.lolstats.model.Champion;

/**
 * The single input both BuildRecommender and RuneRecommender work from:
 * "I'm playing this champion, my allies are this comp, the enemy team is
 * that comp." Produced by either LiveGameStateProvider (from Spectator-V5)
 * or ManualGameStateProvider (from UI selections) - the recommenders
 * themselves don't care which source built it.
 *
 * elapsedGameTimeSeconds is optional context (only meaningful for a live
 * game; manual mode has no real elapsed time) - useful later for
 * time-sensitive advice (e.g. "you're 20 minutes in and still haven't
 * built X"), not required for a first version of either recommender.
 */
public class GameState {

    public enum Source {
        LIVE,
        MANUAL
    }

    private Champion myChampion;
    private TeamComposition allies;
    private TeamComposition enemies;
    private Source source;
    private Long elapsedGameTimeSeconds; // null unless source == LIVE

    public GameState() {
    }

    public GameState(Champion myChampion, TeamComposition allies, TeamComposition enemies, Source source) {
        this.myChampion = myChampion;
        this.allies = allies;
        this.enemies = enemies;
        this.source = source;
    }

    public Champion getMyChampion() {
        return myChampion;
    }

    public void setMyChampion(Champion myChampion) {
        this.myChampion = myChampion;
    }

    public TeamComposition getAllies() {
        return allies;
    }

    public void setAllies(TeamComposition allies) {
        this.allies = allies;
    }

    public TeamComposition getEnemies() {
        return enemies;
    }

    public void setEnemies(TeamComposition enemies) {
        this.enemies = enemies;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Long getElapsedGameTimeSeconds() {
        return elapsedGameTimeSeconds;
    }

    public void setElapsedGameTimeSeconds(Long elapsedGameTimeSeconds) {
        this.elapsedGameTimeSeconds = elapsedGameTimeSeconds;
    }
}
