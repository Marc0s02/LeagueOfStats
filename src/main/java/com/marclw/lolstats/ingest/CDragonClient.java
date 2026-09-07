package com.marclw.lolstats.ingest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to Community Dragon (raw.communitydragon.org) to fetch raw champion,
 * item, and patch-version data.
 *
 * Base URL: https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/
 */
public class CDragonClient {

    // content-metadata.json lives at the site root, not under the v1/ path
    // baseUrl points to - kept as a separate constant rather than derived
    // from baseUrl, since the two paths don't share a prefix.
    private static final String CONTENT_METADATA_URL = "https://raw.communitydragon.org/latest/content-metadata.json";

    private String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CDragonClient() {
    }

    public CDragonClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Fetches champion-summary.json - a lightweight list of every champion's
     * id/name/alias/roles. Does NOT include stats; use fetchChampionDetail()
     * per champion for those. Verified against a real saved response
     * (sample.json in the project root).
     */
    public String fetchChampionData() {
        return get(baseUrl + "champion-summary.json");
    }

    /**
     * Fetches one champion's detail file (base stats, per-level growth,
     * roles, etc.) - champions/{id}.json. Not present in champion-summary.json,
     * so a full champion list with real stats needs one call per champion.
     */
    public String fetchChampionDetail(int championId) {
        return get(baseUrl + "champions/" + championId + ".json");
    }

    /**
     * Fetches items.json (stat bonuses, cost, build paths, inStore/maps/requiredAlly fields).
     */
    public String fetchItemData() {
        return get(baseUrl + "items.json");
    }

    /**
     * Used by CacheManager to check whether the local cache is stale.
     * content-metadata.json returns a JSON object with a top-level
     * "version" string field (verified via a working third-party script
     * that calls .json()["version"] on this exact endpoint).
     */
    public String fetchLatestPatchVersion() {
        String json = get(CONTENT_METADATA_URL);
        // Minimal hand-rolled extraction to avoid pulling in a full parse
        // here - CacheManager/ChampionRepository do real Gson parsing
        // elsewhere. Replace with com.google.gson.JsonParser if you'd
        // rather be consistent throughout.
        int idx = json.indexOf("\"version\"");
        if (idx == -1) {
            throw new IllegalStateException("content-metadata.json response did not contain a \"version\" field");
        }
        int colon = json.indexOf(':', idx);
        int firstQuote = json.indexOf('"', colon);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        return json.substring(firstQuote + 1, secondQuote);
    }

    private String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "GET " + url + " returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching " + url, e);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}