package com.marclw.lolstats.stats;

/**
 * One aggregated row: how championId performed with itemId in its final
 * build specifically when laned against opposingChampionId (same
 * teamPosition-based lane pairing as ChampionMatchupRecord - see
 * MatchupStatsBuilder.tallyLaneMatchups).
 *
 * This is what makes MatchupStatsStore.mostPopularItemIdsForMatchup
 * genuinely matchup-specific rather than falling back to a champion's
 * overall item stats regardless of opponent, which was the previous
 * behaviour (see that method's old TODO).
 *
 * Expect these rows to have MUCH smaller sample sizes than
 * ChampionItemStatsRecord - three keys (champion, opponent, item)
 * fragment a fixed pool of historical matches far more than two keys do.
 * MatchupStatsStore's MIN_SAMPLE_SIZE floor matters more here than
 * anywhere else in this package, and callers should expect this table to
 * often have no data at all for a given triple even when
 * ChampionMatchupRecord and ChampionItemStatsRecord both do.
 */
public class ChampionItemMatchupRecord {

    private int championId;
    private int opposingChampionId;
    private int itemId;
    private int gamesPlayed;
    private int wins;
    private String patchVersion;

    public ChampionItemMatchupRecord() {
    }

    public ChampionItemMatchupRecord(int championId, int opposingChampionId, int itemId,
                                     int gamesPlayed, int wins, String patchVersion) {
        this.championId = championId;
        this.opposingChampionId = opposingChampionId;
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

    public int getOpposingChampionId() {
        return opposingChampionId;
    }

    public void setOpposingChampionId(int opposingChampionId) {
        this.opposingChampionId = opposingChampionId;
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