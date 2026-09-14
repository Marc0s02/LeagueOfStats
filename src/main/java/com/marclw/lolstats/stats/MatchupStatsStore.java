package com.marclw.lolstats.stats;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.marclw.lolstats.recommendation.MatchupStatsProvider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Persists and serves the tables MatchupStatsBuilder produces - the gold
 * layer for the "real match data" toggle. Storage format mirrors
 * CacheManager's gzip-JSON approach for consistency (a proper Postgres/
 * DuckDB-backed FeatureStore was floated for the ML pipeline too; this
 * follows whichever of those actually gets built, rather than
 * prescribing its own separate storage tech now).
 *
 * Implements MatchupStatsProvider directly - StatsAugmentedBuildRecommender
 * depends on that interface, not this concrete class, so a future swap to
 * a real database-backed implementation wouldn't require touching the
 * recommendation package at all.
 */
public class MatchupStatsStore implements MatchupStatsProvider {

    private final Gson gson = new Gson();
    private final Path storageDir;

    private Map<String, ChampionMatchupRecord> matchupsByKey;
    private Map<String, ChampionItemStatsRecord> itemStatsByKey;

    public MatchupStatsStore(Path storageDir) {
        this.storageDir = storageDir;
    }

    public void save(List<ChampionMatchupRecord> matchupRecords, List<ChampionItemStatsRecord> itemRecords) {
        try {
            Files.createDirectories(storageDir);
            writeGzipped(storageDir.resolve("matchup-stats.json.gz"),
                    gson.toJson(matchupRecords, new TypeToken<List<ChampionMatchupRecord>>() {}.getType()));
            writeGzipped(storageDir.resolve("item-stats.json.gz"),
                    gson.toJson(itemRecords, new TypeToken<List<ChampionItemStatsRecord>>() {}.getType()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save matchup stats to " + storageDir, e);
        }
    }

    /**
     * Must be called before any MatchupStatsProvider method - those all
     * read from in-memory maps built here, not from disk on every call.
     * Safe to call again later to pick up a freshly rebuilt table (e.g.
     * after re-running MatchupStatsBuilder on more matches).
     */
    public void load() {
        List<ChampionMatchupRecord> matchupRecords = readGzipped(
                storageDir.resolve("matchup-stats.json.gz"),
                new TypeToken<List<ChampionMatchupRecord>>() {}.getType());
        if (matchupRecords == null) {
            matchupRecords = new ArrayList<>();
        }
        List<ChampionItemStatsRecord> itemRecords = readGzipped(
                storageDir.resolve("item-stats.json.gz"),
                new TypeToken<List<ChampionItemStatsRecord>>() {}.getType());
        if (itemRecords == null) {
            itemRecords = new ArrayList<>();
        }

        matchupsByKey = new HashMap<>();
        for (ChampionMatchupRecord record : matchupRecords) {
            matchupsByKey.put(record.getChampionId() + ":" + record.getOpposingChampionId(), record);
        }

        itemStatsByKey = new HashMap<>();
        for (ChampionItemStatsRecord record : itemRecords) {
            itemStatsByKey.put(record.getChampionId() + ":" + record.getItemId(), record);
        }
    }

    // TODO: pick and enforce an actual minimum-sample-size threshold (e.g.
    // don't report a "100% win rate" backed by 2 games) - every method
    // below currently returns whatever's on record with no floor.

    @Override
    public Double winRateForMatchup(int championId, int opposingChampionId) {
        ChampionMatchupRecord record = matchupsByKey.get(championId + ":" + opposingChampionId);
        return record == null ? null : record.winRatePercent();
    }

    @Override
    public List<Integer> mostPopularItemIdsForMatchup(int championId, int opposingChampionId, int topN) {
        // NOTE: matchup-specific item popularity isn't tracked by
        // MatchupStatsBuilder yet (ChampionItemStatsRecord is deliberately
        // not matchup-specific - see its javadoc on sample-size
        // fragmentation). Falling back to this champion's overall most-
        // built items rather than returning nothing, but that means this
        // currently ignores the opposingChampionId argument entirely.
        return itemStatsByKey.values().stream()
                .filter(record -> record.getChampionId() == championId)
                .sorted(Comparator.comparingInt(ChampionItemStatsRecord::getGamesPlayed).reversed())
                .limit(topN)
                .map(ChampionItemStatsRecord::getItemId)
                .collect(Collectors.toList());
    }

    @Override
    public Double winRateForChampionWithItem(int championId, int itemId) {
        ChampionItemStatsRecord record = itemStatsByKey.get(championId + ":" + itemId);
        return record == null ? null : record.winRatePercent();
    }

    @Override
    public Integer gamesSampledForChampionWithItem(int championId, int itemId) {
        ChampionItemStatsRecord record = itemStatsByKey.get(championId + ":" + itemId);
        return record == null ? null : record.getGamesPlayed();
    }

    private void writeGzipped(Path path, String content) {
        try (Writer writer = new java.io.OutputStreamWriter(
                new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + path, e);
        }
    }

    /**
     * Returns null (rather than an empty collection) if the file itself is
     * missing - e.g. load() called before save() has ever run. Callers
     * that need a list either way (see load() above) null-check explicitly
     * rather than this method silently guessing what "empty" should mean
     * for an arbitrary T.
     */
    private <T> T readGzipped(Path path, Type type) {
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = new java.io.InputStreamReader(
                new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }
}
