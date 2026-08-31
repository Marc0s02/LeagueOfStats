package com.marclw.lolstats.ingest;

/**
 * Talks to Riot's Match-V5 API to fetch completed-match data: final stats
 * (/lol/match/v5/matches/{matchId}) and per-minute timelines
 * (/lol/match/v5/matches/{matchId}/timeline).
 *
 * Used by both:
 *  - training.DatasetBuilder, to bulk-pull historical matches for a sample
 *    of high-elo players/games
 *  - prediction.PredictionService, to pull a just-finished game's data at
 *    request time
 *
 * Requires a Riot Developer API key (rotates every 24h on a dev key) -
 * read from config/environment rather than hardcoded.
 *
 * Regional routing note: match endpoints are routed by continent
 * (americas/europe/asia), not by platform (e.g. euw1/na1) - don't reuse
 * CDragonClient's base URL pattern here.
 */
public class RiotMatchClient {

    private String apiKey;
    private String regionalBaseUrl; // e.g. https://europe.api.riotgames.com

    public RiotMatchClient() {
    }

    public RiotMatchClient(String apiKey, String regionalBaseUrl) {
        this.apiKey = apiKey;
        this.regionalBaseUrl = regionalBaseUrl;
    }

    /**
     * Raw JSON for GET /lol/match/v5/matches/{matchId}.
     */
    public String fetchMatch(String matchId) {
        return null;
    }

    /**
     * Raw JSON for GET /lol/match/v5/matches/{matchId}/timeline.
     */
    public String fetchMatchTimeline(String matchId) {
        return null;
    }

    /**
     * Raw JSON for GET /lol/match/v5/matches/by-puuid/{puuid}/ids - used by
     * DatasetBuilder/HistoricalDatasetLoader to discover match IDs to pull
     * for a given player.
     */
    public String fetchMatchIdsForPuuid(String puuid, int count) {
        return null;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRegionalBaseUrl() {
        return regionalBaseUrl;
    }

    public void setRegionalBaseUrl(String regionalBaseUrl) {
        this.regionalBaseUrl = regionalBaseUrl;
    }
}
