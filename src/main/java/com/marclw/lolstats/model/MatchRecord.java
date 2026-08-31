package com.marclw.lolstats.model;

import java.util.List;

/**
 * A structured, deserialized representation of a single Riot Match-V5
 * "match" response (silver layer) - final per-player and per-team stats
 * for a completed game. Populated by RiotMatchClient via Gson.
 *
 * This is intentionally a thinner, purpose-built shape rather than a
 * direct 1:1 mirror of Riot's raw JSON - only the fields FeatureExtractor
 * actually needs should live here.
 */
public class MatchRecord {

    private String matchId;
    private String gameVersion; // patch, e.g. "14.16"
    private long gameDurationSeconds;
    private String winningTeam; // "BLUE" or "RED"

    private List<PlayerStats> bluePlayers;
    private List<PlayerStats> redPlayers;

    public MatchRecord() {
    }

    /**
     * Per-player final stats for one match - kills/deaths/assists, gold,
     * champion played, damage dealt, vision score, etc.
     */
    public static class PlayerStats {
        private int championId;
        private String summonerName;
        private int kills;
        private int deaths;
        private int assists;
        private int goldEarned;
        private int totalDamageDealtToChampions;
        private int visionScore;

        public PlayerStats() {
        }

        public int getChampionId() {
            return championId;
        }

        public void setChampionId(int championId) {
            this.championId = championId;
        }

        public String getSummonerName() {
            return summonerName;
        }

        public void setSummonerName(String summonerName) {
            this.summonerName = summonerName;
        }

        public int getKills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public void setDeaths(int deaths) {
            this.deaths = deaths;
        }

        public int getAssists() {
            return assists;
        }

        public void setAssists(int assists) {
            this.assists = assists;
        }

        public int getGoldEarned() {
            return goldEarned;
        }

        public void setGoldEarned(int goldEarned) {
            this.goldEarned = goldEarned;
        }

        public int getTotalDamageDealtToChampions() {
            return totalDamageDealtToChampions;
        }

        public void setTotalDamageDealtToChampions(int totalDamageDealtToChampions) {
            this.totalDamageDealtToChampions = totalDamageDealtToChampions;
        }

        public int getVisionScore() {
            return visionScore;
        }

        public void setVisionScore(int visionScore) {
            this.visionScore = visionScore;
        }
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public long getGameDurationSeconds() {
        return gameDurationSeconds;
    }

    public void setGameDurationSeconds(long gameDurationSeconds) {
        this.gameDurationSeconds = gameDurationSeconds;
    }

    public String getWinningTeam() {
        return winningTeam;
    }

    public void setWinningTeam(String winningTeam) {
        this.winningTeam = winningTeam;
    }

    public List<PlayerStats> getBluePlayers() {
        return bluePlayers;
    }

    public void setBluePlayers(List<PlayerStats> bluePlayers) {
        this.bluePlayers = bluePlayers;
    }

    public List<PlayerStats> getRedPlayers() {
        return redPlayers;
    }

    public void setRedPlayers(List<PlayerStats> redPlayers) {
        this.redPlayers = redPlayers;
    }
}
