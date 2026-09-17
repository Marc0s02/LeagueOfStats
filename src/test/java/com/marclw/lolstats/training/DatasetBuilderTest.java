package com.marclw.lolstats.training;

import com.marclw.lolstats.features.FeatureExtractor;
import com.marclw.lolstats.features.TeamAggregator;
import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;
import com.marclw.lolstats.service.StatCalculator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies DatasetBuilder's labeling and its skip-missing-timeline behaviour
 * without touching the network - everything here is hand-built fixtures.
 */
class DatasetBuilderTest {

    private DatasetBuilder builder() {
        return new DatasetBuilder(new FeatureExtractor(new TeamAggregator(), new StatCalculator()));
    }

    private MatchRecord match(String matchId, String winningTeam) {
        MatchRecord record = new MatchRecord();
        record.setMatchId(matchId);
        record.setWinningTeam(winningTeam);
        return record;
    }

    private MatchTimeline timeline(String matchId, int blueGold, int redGold) {
        MatchTimeline.Frame frame = new MatchTimeline.Frame();
        frame.setMinute(15);
        frame.setBlueTotalGold(blueGold);
        frame.setRedTotalGold(redGold);

        MatchTimeline timeline = new MatchTimeline();
        timeline.setMatchId(matchId);
        timeline.setFrames(List.of(frame));
        return timeline;
    }

    @Test
    void labelsBlueWinsAsTrueAndRedWinsAsFalse() {
        Map<String, MatchTimeline> timelines = new HashMap<>();
        timelines.put("M1", timeline("M1", 25000, 20000));
        timelines.put("M2", timeline("M2", 20000, 25000));

        List<FeatureVector> rows = builder().buildTrainingSet(
                List.of(match("M1", "BLUE"), match("M2", "RED")), timelines, 15);

        assertEquals(2, rows.size());
        assertEquals(Boolean.TRUE, rows.get(0).getBlueTeamWon());
        assertEquals(Boolean.FALSE, rows.get(1).getBlueTeamWon());
    }

    @Test
    void skipsMatchesWithNoTimelineRatherThanThrowing() {
        Map<String, MatchTimeline> timelines = new HashMap<>();
        timelines.put("M1", timeline("M1", 25000, 20000));
        // M2 deliberately has no timeline - a real and expected case.

        List<FeatureVector> rows = builder().buildTrainingSet(
                List.of(match("M1", "BLUE"), match("M2", "RED")), timelines, 15);

        assertEquals(1, rows.size(), "match without a timeline should be skipped, not fail the batch");
    }

    @Test
    void usesEachMatchesOwnTimelineNotAnArbitraryOne() {
        // Guards against a plausible bug: looking up timelines by index or
        // iteration order instead of by matchId would pair M2's record with
        // M1's timeline and silently produce garbage training rows.
        Map<String, MatchTimeline> timelines = new HashMap<>();
        timelines.put("M1", timeline("M1", 30000, 10000)); // blue +20000
        timelines.put("M2", timeline("M2", 10000, 30000)); // blue -20000

        List<FeatureVector> rows = builder().buildTrainingSet(
                List.of(match("M1", "BLUE"), match("M2", "RED")), timelines, 15);

        assertEquals(20000.0, rows.get(0).get("goldDiffAtMinute"));
        assertEquals(-20000.0, rows.get(1).get("goldDiffAtMinute"));
    }

    @Test
    void returnsEmptyListForNoMatches() {
        List<FeatureVector> rows = builder().buildTrainingSet(List.of(), new HashMap<>(), 15);
        assertTrue(rows.isEmpty());
    }
}
