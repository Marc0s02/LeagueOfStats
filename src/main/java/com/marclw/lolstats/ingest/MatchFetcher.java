package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pulls a batch of matches (record + timeline) through RiotMatchClient and
 * parses them, with throttling so a personal dev key doesn't get rate-limited
 * partway through a long run.
 *
 * Exists because DatasetBuilder needs a List&lt;MatchRecord&gt; plus a
 * matchId-&gt;MatchTimeline map, and assembling that from the raw client is
 * fiddly enough (two calls per match, 404 handling, throttling, progress
 * reporting) that inlining it into every caller would be wasteful.
 *
 * Rate limits on a Riot DEV key are roughly 20 requests / 1 second and
 * 100 requests / 2 minutes. This class costs 2 requests per match, so the
 * 2-minute window is the binding constraint: ~50 matches per 2 minutes,
 * i.e. about 2.4 seconds per match. DEFAULT_THROTTLE_MILLIS is set a little
 * above that for headroom. A production key has far higher limits and this
 * can be dropped a lot - see setThrottleMillis.
 */
public class MatchFetcher {

    /** Per-match delay. 2 requests/match against a ~100-req/2-min dev key. */
    public static final long DEFAULT_THROTTLE_MILLIS = 2500L;

    private final RiotMatchClient matchClient;
    private final MatchDataParser parser;

    private long throttleMillis = DEFAULT_THROTTLE_MILLIS;

    public MatchFetcher(RiotMatchClient matchClient, MatchDataParser parser) {
        this.matchClient = matchClient;
        this.parser = parser;
    }

    /**
     * Result holder - DatasetBuilder wants the records as a list and the
     * timelines keyed by matchId, so return both rather than making the
     * caller re-index one from the other.
     */
    public static class Batch {
        private final List<MatchRecord> records = new ArrayList<>();
        private final Map<String, MatchTimeline> timelines = new HashMap<>();

        public List<MatchRecord> getRecords() {
            return records;
        }

        public Map<String, MatchTimeline> getTimelines() {
            return timelines;
        }

        public int size() {
            return records.size();
        }
    }

    /**
     * Resolves a Riot ID to a puuid, pulls that account's recent match IDs,
     * then fetches and parses each one.
     *
     * @param gameName Riot ID game name (the part before the #)
     * @param tagLine  Riot ID tag line (the part after the #)
     * @param count    how many recent matches to pull
     */
    public Batch fetchRecentMatchesForRiotId(String gameName, String tagLine, int count) {
        String puuid = matchClient.fetchPuuidByRiotId(gameName, tagLine);
        String idsJson = matchClient.fetchMatchIdsForPuuid(puuid, count);
        return fetchMatches(parseMatchIdArray(idsJson));
    }

    /**
     * Fetches and parses every given match ID. Matches whose detail or
     * timeline 404s are skipped with a note rather than aborting the batch -
     * losing one game out of a few hundred matters far less than losing the
     * whole run, and some matches genuinely have no timeline (see
     * RiotMatchClient.fetchMatchTimeline).
     */
    public Batch fetchMatches(List<String> matchIds) {
        Batch batch = new Batch();
        int index = 0;
        for (String matchId : matchIds) {
            index++;
            try {
                String matchJson = matchClient.fetchMatch(matchId);
                String timelineJson = matchClient.fetchMatchTimeline(matchId);

                MatchRecord record = parser.parseMatchRecord(matchJson);
                MatchTimeline timeline = parser.parseMatchTimeline(timelineJson, matchJson);

                batch.getRecords().add(record);
                batch.getTimelines().put(record.getMatchId(), timeline);

                System.out.printf("  [%d/%d] %s ok%n", index, matchIds.size(), matchId);
            } catch (RiotMatchClient.MatchNotFoundException e) {
                System.out.printf("  [%d/%d] %s skipped (404 - no match or timeline)%n",
                        index, matchIds.size(), matchId);
            } catch (RuntimeException e) {
                // Deliberately broad: a single malformed/unexpected response
                // shouldn't throw away every match already fetched in a run
                // that may have taken many minutes.
                System.out.printf("  [%d/%d] %s skipped (%s)%n",
                        index, matchIds.size(), matchId, e.getMessage());
            }
            throttle();
        }
        return batch;
    }

    /**
     * Parses the JSON string array RiotMatchClient.fetchMatchIdsForPuuid
     * returns. Kept here rather than in MatchDataParser since it isn't
     * really "match data" - it's just a list of IDs.
     */
    public static List<String> parseMatchIdArray(String idsJson) {
        List<String> ids = new ArrayList<>();
        for (var element : com.google.gson.JsonParser.parseString(idsJson).getAsJsonArray()) {
            ids.add(element.getAsString());
        }
        return ids;
    }

    /**
     * Fetches recent matches for several accounts and merges them, so a
     * training corpus isn't drawn from a single player's games (which would
     * bias the model toward that player's rank, champion pool and playstyle).
     *
     * @param riotIds gameName#tagLine strings
     * @param countPerAccount how many recent matches to pull per account
     */
    public Batch fetchRecentMatchesForRiotIds(List<String> riotIds, int countPerAccount) {
        // LinkedHashMap so the same match appearing in two accounts' histories
        // (they played together) is stored once, not twice - duplicate rows
        // would otherwise be silently over-weighted in training.
        Map<String, MatchRecord> uniqueRecords = new LinkedHashMap<>();
        Map<String, MatchTimeline> timelines = new HashMap<>();

        for (String riotId : riotIds) {
            String[] parts = riotId.split("#", 2);
            if (parts.length != 2) {
                System.out.println("Skipping malformed Riot ID (expected gameName#tagLine): " + riotId);
                continue;
            }
            System.out.println("Fetching matches for " + riotId + "...");
            Batch accountBatch = fetchRecentMatchesForRiotId(parts[0], parts[1], countPerAccount);
            for (MatchRecord record : accountBatch.getRecords()) {
                if (uniqueRecords.putIfAbsent(record.getMatchId(), record) == null) {
                    timelines.put(record.getMatchId(), accountBatch.getTimelines().get(record.getMatchId()));
                }
            }
        }

        Batch merged = new Batch();
        merged.getRecords().addAll(uniqueRecords.values());
        merged.getTimelines().putAll(timelines);
        return merged;
    }

    private void throttle() {
        if (throttleMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(throttleMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while throttling between match fetches", e);
        }
    }

    public long getThrottleMillis() {
        return throttleMillis;
    }

    /**
     * Lower this if you get a production API key (much higher limits), or
     * set it to 0 in tests where the client is a stub and no real HTTP
     * happens.
     */
    public void setThrottleMillis(long throttleMillis) {
        this.throttleMillis = throttleMillis;
    }
}