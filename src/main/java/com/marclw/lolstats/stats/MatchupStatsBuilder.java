package com.marclw.lolstats.stats;

import com.marclw.lolstats.model.MatchRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch/offline aggregation job: many MatchRecords (sourced from
 * HistoricalDatasetLoader and/or bulk RiotMatchClient pulls, same bronze
 * data other parts of the pipeline already use) -> two gold-layer tables
 * (ChampionMatchupRecord, ChampionItemStatsRecord), handed to
 * MatchupStatsStore to persist. This is the actual data-engineering
 * artifact for the "real match data" toggle - ingest -> aggregate ->
 * store -> serve, deliberately separate from the ML training pipeline in
 * training/ even though both consume the same MatchRecord input.
 *
 * KNOWN SIMPLIFICATION - matchup pairing: Match-V5 participants can carry
 * a "teamPosition" field (TOP/JUNGLE/MIDDLE/BOTTOM/UTILITY) that would let
 * this pair actual lane opponents, but MatchRecord/MatchDataParser don't
 * capture that field yet. Until they do, buildMatchupStats() below treats
 * EVERY blue player as having "faced" EVERY red player (all 25 cross-team
 * pairs per game) rather than true 1-lane-opponent pairing. That's a real
 * accuracy trade-off worth fixing before leaning on this data too heavily
 * - flagging it here rather than presenting all-pairs matchup data as if
 * it were lane-matchup data.
 */
public class MatchupStatsBuilder {

    public List<ChampionMatchupRecord> buildMatchupStats(List<MatchRecord> matches) {
        // Keyed by "championId:opposingChampionId" -> running [gamesPlayed, wins]
        Map<String, int[]> tally = new HashMap<>();
        Map<String, String> patchByKey = new HashMap<>();

        for (MatchRecord match : matches) {
            boolean blueWon = "BLUE".equals(match.getWinningTeam());
            tallyMatchupsForSide(match.getBluePlayers(), match.getRedPlayers(), blueWon,
                    match.getGameVersion(), tally, patchByKey);
            tallyMatchupsForSide(match.getRedPlayers(), match.getBluePlayers(), !blueWon,
                    match.getGameVersion(), tally, patchByKey);
        }

        List<ChampionMatchupRecord> records = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : tally.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int championId = Integer.parseInt(parts[0]);
            int opposingChampionId = Integer.parseInt(parts[1]);
            int[] gamesAndWins = entry.getValue();
            records.add(new ChampionMatchupRecord(
                    championId, opposingChampionId, gamesAndWins[0], gamesAndWins[1],
                    patchByKey.get(entry.getKey())));
        }
        return records;
    }

    private void tallyMatchupsForSide(List<MatchRecord.PlayerStats> side,
                                       List<MatchRecord.PlayerStats> opposingSide,
                                       boolean sideWon,
                                       String patchVersion,
                                       Map<String, int[]> tally,
                                       Map<String, String> patchByKey) {
        if (side == null || opposingSide == null) {
            return;
        }
        for (MatchRecord.PlayerStats player : side) {
            for (MatchRecord.PlayerStats opponent : opposingSide) {
                String key = player.getChampionId() + ":" + opponent.getChampionId();
                int[] gamesAndWins = tally.computeIfAbsent(key, k -> new int[2]);
                gamesAndWins[0]++; // games
                if (sideWon) {
                    gamesAndWins[1]++; // wins
                }
                patchByKey.put(key, patchVersion);
            }
        }
    }

    /**
     * Not matchup-specific - see ChampionItemStatsRecord javadoc for why.
     * A player contributes one tally entry per item in their final build
     * (item6/trinket included - filtering that out, if desired, is a
     * MatchupStatsStore/query-time concern, not something to bake in here).
     */
    public List<ChampionItemStatsRecord> buildItemStats(List<MatchRecord> matches) {
        Map<String, int[]> tally = new HashMap<>();
        Map<String, String> patchByKey = new HashMap<>();

        for (MatchRecord match : matches) {
            tallyItemsForSide(match.getBluePlayers(), "BLUE".equals(match.getWinningTeam()),
                    match.getGameVersion(), tally, patchByKey);
            tallyItemsForSide(match.getRedPlayers(), "RED".equals(match.getWinningTeam()),
                    match.getGameVersion(), tally, patchByKey);
        }

        List<ChampionItemStatsRecord> records = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : tally.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int championId = Integer.parseInt(parts[0]);
            int itemId = Integer.parseInt(parts[1]);
            int[] gamesAndWins = entry.getValue();
            records.add(new ChampionItemStatsRecord(
                    championId, itemId, gamesAndWins[0], gamesAndWins[1], patchByKey.get(entry.getKey())));
        }
        return records;
    }

    private void tallyItemsForSide(List<MatchRecord.PlayerStats> side,
                                    boolean sideWon,
                                    String patchVersion,
                                    Map<String, int[]> tally,
                                    Map<String, String> patchByKey) {
        if (side == null) {
            return;
        }
        for (MatchRecord.PlayerStats player : side) {
            if (player.getFinalItemIds() == null) {
                continue;
            }
            for (int itemId : player.getFinalItemIds()) {
                String key = player.getChampionId() + ":" + itemId;
                int[] gamesAndWins = tally.computeIfAbsent(key, k -> new int[2]);
                gamesAndWins[0]++;
                if (sideWon) {
                    gamesAndWins[1]++;
                }
                patchByKey.put(key, patchVersion);
            }
        }
    }
}
