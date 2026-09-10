package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;

/**
 * Manual smoke test for the Match-V5 pipeline: fetches one real match +
 * timeline, parses them, and prints the result so you can sanity-check
 * the parsing against what you'd expect from the raw JSON.
 *
 * Usage:
 *   RIOT_API_KEY=your-key-here java SmokeMatch <matchId> [regionalBaseUrl]
 */
public class SmokeMatch {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: SmokeMatch <matchId> [regionalBaseUrl]");
            System.exit(1);
        }
        String matchId = args[0];
        String regionalBaseUrl = args.length > 1 ? args[1] : "https://europe.api.riotgames.com";

        String apiKey = System.getenv("RIOT_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("RIOT_API_KEY environment variable is not set.");
            System.exit(1);
        }

        RiotMatchClient client = new RiotMatchClient(apiKey, regionalBaseUrl);
        MatchDataParser parser = new MatchDataParser();

        System.out.println("Fetching match " + matchId + " from " + regionalBaseUrl + "...");
        String matchJson = client.fetchMatch(matchId);
        MatchRecord record = parser.parseMatchRecord(matchJson);
        printMatchRecord(record);

        System.out.println();
        System.out.println("Fetching timeline for " + matchId + "...");
        try {
            String timelineJson = client.fetchMatchTimeline(matchId);
            MatchTimeline timeline = parser.parseMatchTimeline(timelineJson, matchJson);
            printMatchTimeline(timeline);
        } catch (RiotMatchClient.MatchNotFoundException e) {
            System.out.println("No timeline available for this match (404) - "
                    + "expected for some very old matches or non-standard game modes.");
        }
    }

    private static void printMatchRecord(MatchRecord record) {
        System.out.println("=== MatchRecord ===");
        System.out.println("matchId:      " + record.getMatchId());
        System.out.println("gameVersion:  " + record.getGameVersion());
        System.out.println("duration(s):  " + record.getGameDurationSeconds());
        System.out.println("winningTeam:  " + record.getWinningTeam());

        System.out.println("-- Blue --");
        for (MatchRecord.PlayerStats p : record.getBluePlayers()) {
            printPlayer(p);
        }
        System.out.println("-- Red --");
        for (MatchRecord.PlayerStats p : record.getRedPlayers()) {
            printPlayer(p);
        }
    }

    private static void printPlayer(MatchRecord.PlayerStats p) {
        System.out.printf(
                "  championId=%d  name=%-16s KDA=%d/%d/%d  gold=%d  dmg=%d  vision=%d%n",
                p.getChampionId(), p.getSummonerName(), p.getKills(), p.getDeaths(), p.getAssists(),
                p.getGoldEarned(), p.getTotalDamageDealtToChampions(), p.getVisionScore());
    }

    private static void printMatchTimeline(MatchTimeline timeline) {
        System.out.println("=== MatchTimeline ===");
        System.out.println("matchId:  " + timeline.getMatchId());
        System.out.println("frames:   " + timeline.getFrames().size());
        System.out.printf("%-4s %10s %10s %8s %8s %7s %7s %7s %7s %6s %6s%n",
                "min", "blueGold", "redGold", "blueXp", "redXp",
                "bDrag", "rDrag", "bHer", "rHer", "bKill", "rKill");
        for (MatchTimeline.Frame f : timeline.getFrames()) {
            System.out.printf("%-4d %10d %10d %8d %8d %7d %7d %7d %7d %6d %6d%n",
                    f.getMinute(), f.getBlueTotalGold(), f.getRedTotalGold(),
                    f.getBlueTotalXp(), f.getRedTotalXp(),
                    f.getBlueDragonKills(), f.getRedDragonKills(),
                    f.getBlueHeraldKills(), f.getRedHeraldKills(),
                    f.getBlueChampionKills(), f.getRedChampionKills());
        }
    }
}