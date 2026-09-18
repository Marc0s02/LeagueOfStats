package com.marclw.lolstats.stats;

/**
 * One aggregated row: how championId performed in games where itemId
 * appeared in its final build (MatchRecord.PlayerStats.finalItemIds),
 * across every historical match MatchupStatsBuilder processed. Not
 * matchup-specific - deliberately simpler/coarser than
 * ChampionMatchupRecord, since "champion+item+opponent" triples would
 * fragment the sample size too thin to be meaningful for most
 * champion/item pairs given a realistically-sized historical dataset.
 */
public class ChampionItemStatsRecord {

    private int championId;
    private int itemId;
    private int gamesPlayed;
    private int wins;
    private String patchVersion;

    public ChampionItemStatsRecord() {
    }

    public ChampionItemStatsRecord(int championId, int itemId, int gamesPlayed, int wins, String patchVersion) {
        this.championId = championId;
        this.itemId = itemId;
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

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
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
