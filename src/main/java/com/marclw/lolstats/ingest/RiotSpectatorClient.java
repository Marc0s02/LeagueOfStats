package com.marclw.lolstats.ingest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to Riot's Spectator-V5 API to check whether a given summoner is
 * currently in an active game, and to fetch that game's current state
 * (champion picks, current game time) via
 * /lol/spectator/v5/active-games/by-summoner/{puuid}.
 *
 * IMPORTANT LIMITATION: Spectator-V5 only exposes champion picks/bans and
 * elapsed game time - NOT live gold/XP/objective counts. Every feature the
 * win-probability model is trained on (gold diff, XP diff, kills, dragons,
 * heralds, towers) comes from Match-V5 timeline frames, which don't exist
 * for an in-progress game. So this client can answer "who is playing whom
 * right now", but it cannot feed the model. PredictionService therefore
 * scores FINISHED matches as-of-a-minute instead, and that distinction is
 * worth stating plainly rather than implying live prediction works.
 *
 * Platform-routed (e.g. euw1/na1), unlike RiotMatchClient's regional
 * routing - keep these base URLs separate.
 */
public class RiotSpectatorClient {

    private String apiKey;
    private String platformBaseUrl; // e.g. https://euw1.api.riotgames.com

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public RiotSpectatorClient() {
    }

    public RiotSpectatorClient(String apiKey, String platformBaseUrl) {
        this.apiKey = apiKey;
        this.platformBaseUrl = platformBaseUrl;
    }

    /**
     * Raw JSON for GET /lol/spectator/v5/active-games/by-summoner/{puuid},
     * or null if the summoner isn't currently in a game.
     *
     * Returns null rather than throwing on 404 because "not in a game" is
     * the normal, expected answer here most of the time - unlike a missing
     * match in RiotMatchClient, where a 404 is genuinely exceptional.
     */
    public String fetchActiveGame(String puuid) {
        String url = platformBaseUrl + "/lol/spectator/v5/active-games/by-summoner/" + puuid;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Riot-Token", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return null; // not in a game
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "GET " + url + " returned HTTP " + response.statusCode()
                                + " - check your API key hasn't expired (dev keys last 24h)");
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching " + url, e);
        }
    }

    /**
     * Convenience: true if the summoner is currently in a game.
     */
    public boolean isInGame(String puuid) {
        return fetchActiveGame(puuid) != null;
    }

    /**
     * Seconds elapsed in the summoner's current game, or -1 if they aren't
     * in one. Note Riot reports gameLength as 0 during champion select and
     * the loading screen, so a fresh game can legitimately report 0.
     */
    public long fetchGameLengthSeconds(String puuid) {
        String json = fetchActiveGame(puuid);
        if (json == null) {
            return -1;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.has("gameLength") ? root.get("gameLength").getAsLong() : 0;
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