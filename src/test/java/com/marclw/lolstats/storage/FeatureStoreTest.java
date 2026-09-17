package com.marclw.lolstats.storage;

import com.marclw.lolstats.features.FeatureSpec;
import com.marclw.lolstats.model.FeatureVector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureStoreTest {

    private FeatureVector row(double goldDiff, Boolean blueWon) {
        FeatureVector vector = new FeatureVector();
        for (String name : FeatureSpec.FEATURE_NAMES) {
            vector.put(name, 0.0);
        }
        vector.put("goldDiffAtMinute", goldDiff);
        vector.setBlueTeamWon(blueWon);
        return vector;
    }

    @Test
    void roundTripsRowsThroughCsv(@TempDir Path tempDir) {
        FeatureStore store = new FeatureStore(tempDir.resolve("features.csv"));
        List<FeatureVector> written = List.of(row(1234.5, true), row(-987.5, false));

        store.writeFeatureTable(written);
        List<FeatureVector> read = store.readFeatureTable();

        assertEquals(2, read.size());
        assertEquals(1234.5, read.get(0).get("goldDiffAtMinute"));
        assertEquals(Boolean.TRUE, read.get(0).getBlueTeamWon());
        assertEquals(-987.5, read.get(1).get("goldDiffAtMinute"));
        assertEquals(Boolean.FALSE, read.get(1).getBlueTeamWon());
    }

    @Test
    void writesHeaderInFeatureSpecOrder(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("features.csv");
        new FeatureStore(file).writeFeatureTable(List.of(row(1, true)));

        String header = Files.readAllLines(file).get(0);
        String expected = String.join(",", FeatureSpec.FEATURE_NAMES) + ",blueTeamWon";
        assertEquals(expected, header,
                "column order must match FeatureSpec or the model sees shuffled features");
    }

    @Test
    void preservesEveryFeatureColumnNotJustTheOnesThatWereSet(@TempDir Path tempDir) {
        FeatureStore store = new FeatureStore(tempDir.resolve("features.csv"));
        FeatureVector vector = new FeatureVector();
        for (int i = 0; i < FeatureSpec.FEATURE_NAMES.size(); i++) {
            vector.put(FeatureSpec.FEATURE_NAMES.get(i), i * 1.5);
        }
        vector.setBlueTeamWon(true);

        store.writeFeatureTable(List.of(vector));
        FeatureVector read = store.readFeatureTable().get(0);

        for (int i = 0; i < FeatureSpec.FEATURE_NAMES.size(); i++) {
            assertEquals(i * 1.5, read.get(FeatureSpec.FEATURE_NAMES.get(i)),
                    "lost or corrupted column " + FeatureSpec.FEATURE_NAMES.get(i));
        }
    }

    @Test
    void unlabeledRowsRoundTripAsNullNotFalse(@TempDir Path tempDir) {
        // Important distinction: an unlabeled serving row read back as
        // "false" would look like a red win and quietly poison any training
        // set it got mixed into.
        FeatureStore store = new FeatureStore(tempDir.resolve("features.csv"));
        store.writeFeatureTable(List.of(row(1, null)));

        assertNull(store.readFeatureTable().get(0).getBlueTeamWon());
    }

    @Test
    void writingEmptyListProducesHeaderOnlyFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("features.csv");
        new FeatureStore(file).writeFeatureTable(List.of());

        assertTrue(Files.exists(file));
        assertEquals(1, Files.readAllLines(file).size());
        assertTrue(new FeatureStore(file).readFeatureTable().isEmpty());
    }
}
