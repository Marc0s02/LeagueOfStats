package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.MatchRecord;

import java.nio.file.Path;
import java.util.List;

/**
 * Bulk-loads historical match data from a public dataset file (e.g. an
 * Oracle's Elixir pro-play CSV, or a Kaggle match-history export) rather
 * than pulling every match individually through RiotMatchClient.
 *
 * Existing purely to make DatasetBuilder's training corpus bigger/cheaper
 * to assemble than API-only pulls would allow (the public Riot API is
 * rate-limited fairly aggressively on a dev key) - not required if the
 * project ends up sourcing all training data via the API instead.
 *
 * Whatever file format is chosen, this class's job is to normalize rows
 * into the same MatchRecord shape RiotMatchClient produces, so
 * DatasetBuilder/FeatureExtractor don't need to know which source a given
 * match came from.
 */
public class HistoricalDatasetLoader {

    private Path datasetFile;

    public HistoricalDatasetLoader() {
    }

    public HistoricalDatasetLoader(Path datasetFile) {
        this.datasetFile = datasetFile;
    }

    /**
     * Parses the configured dataset file into a list of MatchRecords.
     * Exact parsing logic depends on which dataset format is chosen -
     * Oracle's Elixir is one row per player per game, so this will likely
     * need to group rows by game ID before producing one MatchRecord per
     * match.
     */
    public List<MatchRecord> loadMatches() {
        return null;
    }

    public Path getDatasetFile() {
        return datasetFile;
    }

    public void setDatasetFile(Path datasetFile) {
        this.datasetFile = datasetFile;
    }
}
