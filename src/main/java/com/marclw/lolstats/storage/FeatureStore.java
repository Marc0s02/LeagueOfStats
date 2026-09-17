package com.marclw.lolstats.storage;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists/reloads the gold-layer feature table DatasetBuilder produces.
 *
 * A flat CSV (one row per FeatureVector, columns from FeatureSpec.FEATURE_NAMES
 * plus a blueTeamWon label column) is a perfectly legitimate choice here
 * and keeps the project's storage story simple/inspectable; a real
 * database (Postgres/DuckDB) is a reasonable upgrade if the elective wants
 * to see a proper warehouse layer, but isn't required for the pipeline to
 * work correctly.
 */
public class FeatureStore {

    private static final String LABEL_COLUMN = "blueTeamWon";

    private java.nio.file.Path storageLocation;

    public FeatureStore() {
    }

    public FeatureStore(java.nio.file.Path storageLocation) {
        this.storageLocation = storageLocation;
    }

    /**
     * Overwrites the file at storageLocation with the given rows. Column
     * order is FeatureSpec.FEATURE_NAMES followed by blueTeamWon, so the
     * file stays readable/diffable independent of Map iteration order.
     */
    public void writeFeatureTable(List<FeatureVector> rows) {
        List<String> headers = new ArrayList<>(FeatureSpec.FEATURE_NAMES);
        headers.add(LABEL_COLUMN);

        try (Writer writer = Files.newBufferedWriter(storageLocation, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder().setHeader(headers.toArray(new String[0])).build())) {
            for (FeatureVector row : rows) {
                List<Object> values = new ArrayList<>();
                for (String featureName : FeatureSpec.FEATURE_NAMES) {
                    values.add(row.get(featureName));
                }
                // blueTeamWon may be null for a row that was never labeled
                // (shouldn't happen for training rows DatasetBuilder
                // produces, but writing "" rather than throwing keeps this
                // method from being training-set-only by assumption).
                values.add(row.getBlueTeamWon() == null ? "" : row.getBlueTeamWon());
                printer.printRecord(values);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write feature table to " + storageLocation, e);
        }
    }

    /**
     * Reads back whatever writeFeatureTable last wrote to storageLocation.
     * Only recognizes columns FeatureSpec currently knows about - a file
     * written by an older FeatureSpec version with extra/missing columns
     * isn't validated here (see FeatureSpec's note on ModelRegistry
     * versioning for where that check belongs instead).
     */
    public List<FeatureVector> readFeatureTable() {
        List<FeatureVector> rows = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(storageLocation, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                     .parse(reader)) {
            for (CSVRecord record : parser) {
                FeatureVector vector = new FeatureVector();
                for (String featureName : FeatureSpec.FEATURE_NAMES) {
                    vector.put(featureName, Double.parseDouble(record.get(featureName)));
                }
                String label = record.get(LABEL_COLUMN);
                vector.setBlueTeamWon(label.isEmpty() ? null : Boolean.parseBoolean(label));
                rows.add(vector);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read feature table from " + storageLocation, e);
        }
        return rows;
    }

    public java.nio.file.Path getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(java.nio.file.Path storageLocation) {
        this.storageLocation = storageLocation;
    }
}