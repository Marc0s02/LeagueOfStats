package com.marclw.lolstats.stats;

/**
 * One aggregated row: how championId performed specifically when facing
 * opposingChampionId, across every historical match MatchupStatsBuilder
 * processed. This is the gold-layer artifact for the "matchup win rate"
 * half of MatchupStatsProvider - a flat table of these is what
 * MatchupStatsStore persists/loads/queries.
 *
 * Symmetric pairs are stored as two separate rows (championId=A,
 * opposingChampionId=B) and (championId=B, opposingChampionId=A) rather
 * than one row you'd have to flip depending on perspective - simpler to
 * query, at the cost of double the row count.
 */
public class ChampionMatchupRecord {

    private int championId;
    private int opposingChampionId;
    private int gamesPlayed;
    private int wins;
    private String patchVersion; // which patch this row was aggregated from, for later filtering by recency

    public ChampionMatchupRecord() {
    }

    public ChampionMatchupRecord(int championId, int opposingChampionId, int gamesPlayed, int wins, String patchVersion) {
        this.championId = championId;
        this.opposingChampionId = opposingChampionId;
        this.gamesPlayed = gamesPlayed;
        this.wins = wins;
        this.patchVersion = patchVersion;
    }

    public double winRatePercent() {
        return gamesPlayed == 0 ? 0.0 : (100.0 * wins / gamesPlayed);
    }

    public int getChampionId() {
        return championId;
    }

    public void setChampionId(int championId) {
        this.championId = championId;
    }

    public int getOpposingChampionId() {
        return opposingChampionId;
    }

    public void setOpposingChampionId(int opposingChampionId) {
        this.opposingChampionId = opposingChampionId;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public String getPatchVersion() {
        return patchVersion;
    }

    public void setPatchVersion(String patchVersion) {
        this.patchVersion = patchVersion;
    }
}
