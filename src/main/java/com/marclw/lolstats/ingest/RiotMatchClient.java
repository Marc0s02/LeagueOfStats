package com.marclw.lolstats.ingest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to Riot's Match-V5 API. Regionally routed (americas/europe/asia/sea),
 * NOT platform-routed (euw1/na1) - a different host family than
 * RiotSpectatorClient uses. See regionalBaseUrl.
 *
 * API key: never hardcode it or commit it. Pass it in via an environment
 * variable (e.g. RIOT_API_KEY) read in App.java, not a literal string here.
 * Riot dev keys also expire every 24 hours - if requests start failing with
 * 401/403 after working fine yesterday, regenerate the key first before
 * assuming something broke in code.
 */
public class RiotMatchClient {

    private String apiKey;
    private String regionalBaseUrl; // e.g. https://europe.api.riotgames.com

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
        return get(regionalBaseUrl + "/lol/match/v5/matches/" + matchId);
    }

    /**
     * Raw JSON for GET /lol/match/v5/matches/{matchId}/timeline.
     * Not every match has a timeline (very old matches, some game modes) -
     * callers should handle a 404 gracefully rather than assume it always
     * succeeds if fetchMatch() did.
     */
    public String fetchMatchTimeline(String matchId) {
        return get(regionalBaseUrl + "/lol/match/v5/matches/" + matchId + "/timeline");
    }

    /**
     * Raw JSON array of match ID strings for
     * GET /lol/match/v5/matches/by-puuid/{puuid}/ids?count={count}.
     */
    public String fetchMatchIdsForPuuid(String puuid, int count) {
        return get(regionalBaseUrl + "/lol/match/v5/matches/by-puuid/" + puuid + "/ids?count=" + count);
    }

    private String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Riot-Token", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new MatchNotFoundException(url);
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
     * Thrown on a 404 specifically, so callers (e.g. skip-missing-timeline
     * logic in DatasetBuilder) can catch this distinctly from a real error
     * like a bad API key or network failure.
     */
    public static class MatchNotFoundException extends RuntimeException {
        public MatchNotFoundException(String url) {
            super("Not found (404): " + url);
        }
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