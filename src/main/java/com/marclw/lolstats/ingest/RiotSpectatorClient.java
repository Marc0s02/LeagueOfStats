package com.marclw.lolstats.ingest;

/**
 * Talks to Riot's Spectator-V5 API to check whether a given summoner is
 * currently in an active game, and to fetch that game's current state
 * (champion picks, current game time) via
 * /lol/spectator/v5/active-games/by-summoner/{puuid}.
 *
 * Note: Spectator-V5 only exposes champion picks/bans and elapsed game
 * time, not live gold/XP/objective counts - it's useful for "who's playing
 * who right now", but PredictionController's live view will likely need to
 * fall back to periodically re-fetching a recently-finished match via
 * RiotMatchClient for the richer minute-by-minute features, rather than
 * relying on Spectator-V5 alone.
 *
 * Platform-routed (e.g. euw1/na1), unlike RiotMatchClient's regional
 * routing - keep these base URLs separate.
 */
public class RiotSpectatorClient {

    private String apiKey;
    private String platformBaseUrl; // e.g. https://euw1.api.riotgames.com

    public RiotSpectatorClient() {
    }

    public RiotSpectatorClient(String apiKey, String platformBaseUrl) {
        this.apiKey = apiKey;
        this.platformBaseUrl = platformBaseUrl;
    }

    /**
     * Raw JSON for GET /lol/spectator/v5/active-games/by-summoner/{puuid}.
     * Returns null (or throws, depending on how you wire error handling)
     * if the summoner isn't currently in a game.
     */
    public String fetchActiveGame(String puuid) {
        return null;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPlatformBaseUrl() {
        return platformBaseUrl;
    }

    public void setPlatformBaseUrl(String platformBaseUrl) {
        this.platformBaseUrl = platformBaseUrl;
    }
}
