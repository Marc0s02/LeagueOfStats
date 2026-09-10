package com.marclw.lolstats.ingest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchDataParser {

    public MatchRecord parseMatchRecord(String matchJson) {
        JsonObject root = JsonParser.parseString(matchJson).getAsJsonObject();
        JsonObject metadata = root.getAsJsonObject("metadata");
        JsonObject info = root.getAsJsonObject("info");

        MatchRecord record = new MatchRecord();
        record.setMatchId(metadata.get("matchId").getAsString());
        record.setGameVersion(info.get("gameVersion").getAsString());
        record.setGameDurationSeconds(info.get("gameDuration").getAsLong());

        List<MatchRecord.PlayerStats> bluePlayers = new ArrayList<>();
        List<MatchRecord.PlayerStats> redPlayers = new ArrayList<>();
        String winningTeam = null;

        for (var teamElement : info.getAsJsonArray("teams")) {
            JsonObject team = teamElement.getAsJsonObject();
            if (team.get("win").getAsBoolean()) {
                winningTeam = team.get("teamId").getAsInt() == 100 ? "BLUE" : "RED";
            }
        }
        record.setWinningTeam(winningTeam);

        for (var participantElement : info.getAsJsonArray("participants")) {
            JsonObject p = participantElement.getAsJsonObject();
            MatchRecord.PlayerStats stats = new MatchRecord.PlayerStats();
            stats.setChampionId(p.get("championId").getAsInt());
            // riotIdGameName replaced summonerName in more recent API
            // versions - fall back if the newer field isn't present.
            stats.setSummonerName(p.has("riotIdGameName")
                    ? p.get("riotIdGameName").getAsString()
                    : p.get("summonerName").getAsString());
            stats.setKills(p.get("kills").getAsInt());
            stats.setDeaths(p.get("deaths").getAsInt());
            stats.setAssists(p.get("assists").getAsInt());
            stats.setGoldEarned(p.get("goldEarned").getAsInt());
            stats.setTotalDamageDealtToChampions(p.get("totalDamageDealtToChampions").getAsInt());
            stats.setVisionScore(p.get("visionScore").getAsInt());

            if (p.get("teamId").getAsInt() == 100) {
                bluePlayers.add(stats);
            } else {
                redPlayers.add(stats);
            }
        }
        record.setBluePlayers(bluePlayers);
        record.setRedPlayers(redPlayers);
        return record;
    }

    /**
     * Needs the match detail JSON alongside the timeline JSON purely to
     * build a participantId -> team mapping (timeline events only carry
     * participantId, 1-10, not teamId directly) - deliberately NOT relying
     * on the common "1-5 is blue, 6-10 is red" convention, since that's an
     * assumption rather than a guarantee across all game modes.
     */
    public MatchTimeline parseMatchTimeline(String timelineJson, String matchJson) {
        Map<Integer, Boolean> isBlueByParticipantId = buildParticipantTeamMap(matchJson);

        JsonObject root = JsonParser.parseString(timelineJson).getAsJsonObject();
        JsonObject metadata = root.getAsJsonObject("metadata");
        JsonArray framesJson = root.getAsJsonObject("info").getAsJsonArray("frames");

        MatchTimeline timeline = new MatchTimeline();
        timeline.setMatchId(metadata.get("matchId").getAsString());

        List<MatchTimeline.Frame> frames = new ArrayList<>();
        // Running totals, since dragon/herald/tower/champion kills are
        // cumulative-per-team across the game, not reset each frame.
        int blueDragons = 0, redDragons = 0;
        int blueHeralds = 0, redHeralds = 0;
        int blueTowers = 0, redTowers = 0;
        int blueKills = 0, redKills = 0;

        for (var frameElement : framesJson) {
            JsonObject frameJson = frameElement.getAsJsonObject();
            JsonObject participantFrames = frameJson.getAsJsonObject("participantFrames");

            int blueGold = 0, redGold = 0, blueXp = 0, redXp = 0;
            for (String participantIdStr : participantFrames.keySet()) {
                JsonObject pf = participantFrames.getAsJsonObject(participantIdStr);
                boolean isBlue = isBlueByParticipantId.getOrDefault(Integer.parseInt(participantIdStr), true);
                int totalGold = pf.get("totalGold").getAsInt();
                int xp = pf.get("xp").getAsInt();
                if (isBlue) {
                    blueGold += totalGold;
                    blueXp += xp;
                } else {
                    redGold += totalGold;
                    redXp += xp;
                }
            }

            for (var eventElement : frameJson.getAsJsonArray("events")) {
                JsonObject event = eventElement.getAsJsonObject();
                String type = event.get("type").getAsString();

                switch (type) {
                    case "CHAMPION_KILL" -> {
                        int killerId = event.has("killerId") ? event.get("killerId").getAsInt() : 0;
                        if (killerId != 0) { // 0 = environmental death, no credit
                            if (isBlueByParticipantId.getOrDefault(killerId, true)) {
                                blueKills++;
                            } else {
                                redKills++;
                            }
                        }
                    }
                    case "ELITE_MONSTER_KILL" -> {
                        int killerId = event.has("killerId") ? event.get("killerId").getAsInt() : 0;
                        String monsterType = event.has("monsterType") ? event.get("monsterType").getAsString() : "";
                        boolean isBlue = isBlueByParticipantId.getOrDefault(killerId, true);
                        if ("DRAGON".equals(monsterType)) {
                            if (isBlue) blueDragons++; else redDragons++;
                        } else if ("RIFTHERALD".equals(monsterType)) {
                            if (isBlue) blueHeralds++; else redHeralds++;
                        }
                    }
                    case "BUILDING_KILL" -> {
                        if ("TOWER_BUILDING".equals(event.get("buildingType").getAsString())) {
                            // teamId here is the team that OWNED the destroyed
                            // tower - so credit goes to the OTHER team.
                            int ownerTeamId = event.get("teamId").getAsInt();
                            if (ownerTeamId == 100) redTowers++; else blueTowers++;
                        }
                    }
                    default -> {
                        // Other event types (WARD_PLACED, ITEM_PURCHASED, etc.)
                        // aren't tracked for feature purposes right now.
                    }
                }
            }

            MatchTimeline.Frame frame = new MatchTimeline.Frame();
            frame.setMinute((int) (frameJson.get("timestamp").getAsLong() / 60000));
            frame.setBlueTotalGold(blueGold);
            frame.setRedTotalGold(redGold);
            frame.setBlueTotalXp(blueXp);
            frame.setRedTotalXp(redXp);
            frame.setBlueDragonKills(blueDragons);
            frame.setRedDragonKills(redDragons);
            frame.setBlueHeraldKills(blueHeralds);
            frame.setRedHeraldKills(redHeralds);
            frame.setBlueTowersDestroyed(blueTowers);
            frame.setRedTowersDestroyed(redTowers);
            frame.setBlueChampionKills(blueKills);
            frame.setRedChampionKills(redKills);
            frames.add(frame);
        }

        timeline.setFrames(frames);
        return timeline;
    }

    private Map<Integer, Boolean> buildParticipantTeamMap(String matchJson) {
        JsonObject info = JsonParser.parseString(matchJson).getAsJsonObject().getAsJsonObject("info");
        Map<Integer, Boolean> map = new HashMap<>();
        for (var participantElement : info.getAsJsonArray("participants")) {
            JsonObject p = participantElement.getAsJsonObject();
            map.put(p.get("participantId").getAsInt(), p.get("teamId").getAsInt() == 100);
        }
        return map;
    }
}