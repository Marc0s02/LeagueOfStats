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
 * Matchup pairing uses MatchRecord.PlayerStats.teamPosition to pair real
 * lane opponents (TOP vs TOP, JUNGLE vs JUNGLE, etc.) rather than every
 * blue player against every red player. Games where teamPosition is
 * missing/blank for a player (non-standard modes, or older matches predating
 * the field) are skipped for that player's pairing rather than guessed at -
 * a silently-wrong lane pairing is worse than a dropped data point here.
 */
public class MatchupStatsBuilder {

    public List<ChampionMatchupRecord> buildMatchupStats(List<MatchRecord> matches) {
        // Keyed by "championId:opposingChampionId" -> running [gamesPlayed, wins]
        Map<String, int[]> tally = new HashMap<>();
        Map<String, String> patchByKey = new HashMap<>();

        for (MatchRecord match : matches) {
            boolean blueWon = "BLUE".equals(match.getWinningTeam());
            tallyLaneMatchups(match.getBluePlayers(), match.getRedPlayers(), blueWon,
                    match.getGameVersion(), tally, patchByKey);
            tallyLaneMatchups(match.getRedPlayers(), match.getBluePlayers(), !blueWon,
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

    /**
     * Pairs each player on `side` with the opponent on `opposingSide` who
     * shares the same teamPosition - real lane opponents, not a cross
     * product. A player whose teamPosition is missing/blank, or who has no
     * positional counterpart on the other side (shouldn't normally happen
     * in a standard 5v5 game, but data can be messy), contributes no
     * matchup tally rather than being paired arbitrarily.
     */
    private void tallyLaneMatchups(List<MatchRecord.PlayerStats> side,
                                   List<MatchRecord.PlayerStats> opposingSide,
                                   boolean sideWon,
                                   String patchVersion,
                                   Map<String, int[]> tally,
                                   Map<String, String> patchByKey) {
        if (side == null || opposingSide == null) {
            return;
        }
        for (MatchRecord.PlayerStats player : side) {
            String position = player.getTeamPosition();
            if (position == null || position.isBlank()) {
                continue;
            }
            MatchRecord.PlayerStats laneOpponent = findByPosition(opposingSide, position);
            if (laneOpponent == null) {
                continue;
            }
            String key = player.getChampionId() + ":" + laneOpponent.getChampionId();
            int[] gamesAndWins = tally.computeIfAbsent(key, k -> new int[2]);
            gamesAndWins[0]++; // games
            if (sideWon) {
                gamesAndWins[1]++; // wins
            }
            patchByKey.put(key, patchVersion);
        }
    }

    private MatchRecord.PlayerStats findByPosition(List<MatchRecord.PlayerStats> players, String position) {
        for (MatchRecord.PlayerStats player : players) {
            if (position.equals(player.getTeamPosition())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Same lane-pairing rule as buildMatchupStats, but tallying per
     * (championId, opposingChampionId, itemId) triple rather than just
     * (championId, opposingChampionId) - this is what feeds
     * MatchupStatsStore.mostPopularItemIdsForMatchup so it can be
     * genuinely matchup-specific instead of falling back to a champion's
     * overall build stats regardless of opponent.
     */
    public List<ChampionItemMatchupRecord> buildItemMatchupStats(List<MatchRecord> matches) {
        // Keyed by "championId:opposingChampionId:itemId" -> [gamesPlayed, wins]
        Map<String, int[]> tally = new HashMap<>();
        Map<String, String> patchByKey = new HashMap<>();

        for (MatchRecord match : matches) {
            boolean blueWon = "BLUE".equals(match.getWinningTeam());
            tallyItemMatchupsForSide(match.getBluePlayers(), match.getRedPlayers(), blueWon,
                    match.getGameVersion(), tally, patchByKey);
            tallyItemMatchupsForSide(match.getRedPlayers(), match.getBluePlayers(), !blueWon,
                    match.getGameVersion(), tally, patchByKey);
        }

        List<ChampionItemMatchupRecord> records = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : tally.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int championId = Integer.parseInt(parts[0]);
            int opposingChampionId = Integer.parseInt(parts[1]);
            int itemId = Integer.parseInt(parts[2]);
            int[] gamesAndWins = entry.getValue();
            records.add(new ChampionItemMatchupRecord(
                    championId, opposingChampionId, itemId, gamesAndWins[0], gamesAndWins[1],
                    patchByKey.get(entry.getKey())));
        }
        return records;
    }

    /**
     * Pairs lane opponents exactly like tallyLaneMatchups (same
     * teamPosition rule, same skip-on-missing-position/opponent
     * behaviour), then additionally tallies each of the player's
     * finalItemIds against that specific opponent rather than just the
     * outcome.
     */
    private void tallyItemMatchupsForSide(List<MatchRecord.PlayerStats> side,
                                          List<MatchRecord.PlayerStats> opposingSide,
                                          boolean sideWon,
                                          String patchVersion,
                                          Map<String, int[]> tally,
                                          Map<String, String> patchByKey) {
        if (side == null || opposingSide == null) {
            return;
        }
        for (MatchRecord.PlayerStats player : side) {
            String position = player.getTeamPosition();
            if (position == null || position.isBlank()) {
                continue;
            }
            MatchRecord.PlayerStats laneOpponent = findByPosition(opposingSide, position);
            if (laneOpponent == null || player.getFinalItemIds() == null) {
                continue;
            }
            for (int itemId : player.getFinalItemIds()) {
                String key = player.getChampionId() + ":" + laneOpponent.getChampionId() + ":" + itemId;
                int[] gamesAndWins = tally.computeIfAbsent(key, k -> new int[2]);
                gamesAndWins[0]++;
                if (sideWon) {
                    gamesAndWins[1]++;
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