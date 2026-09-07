package com.marclw.lolstats.ingest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to Riot's official Data Dragon CDN (ddragon.leagueoflegends.com)
 * for champion base stats and item data. Unlike Community Dragon's
 * reverse-engineered endpoints, Data Dragon's schema is officially
 * documented and stable - used here specifically because
 * champions/{id}.json on Community Dragon turned out to contain no
 * combat stats at all (lore/abilities/skins only).
 *
 * Bonus: champion.json returns ALL champions' stats in one request,
 * replacing what would otherwise be one CDragon request per champion.
 */
public class DataDragonClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * versions.json is a JSON array of version strings, newest first -
     * e.g. ["14.17.1", "14.16.1", ...]. Returns just the first (latest).
     */
    public String fetchLatestVersion() {
        String json = get("https://ddragon.leagueoflegends.com/api/versions.json");
        int firstQuote = json.indexOf('"', json.indexOf('['));
        int secondQuote = json.indexOf('"', firstQuote + 1);
        return json.substring(firstQuote + 1, secondQuote);
    }

    /**
     * Returns the full champion.json for the given patch version - an
     * object shaped like { "data": { "Aatrox": {...}, "Ahri": {...}, ... } },
     * each entry keyed by champion name (not numeric id) and containing a
     * numeric "key" field plus a "stats" object.
     */
    public String fetchChampionData(String version) {
        return get("https://ddragon.leagueoflegends.com/cdn/" + version + "/data/en_US/champion.json");
    }

    /**
     * Returns the full item.json for the given patch version - shaped like
     * { "data": { "1001": {...}, "3031": {...}, ... } }, keyed by numeric
     * item id as a string, each with "gold" (cost) and "stats" objects.
     */
    public String fetchItemData(String version) {
        return get("https://ddragon.leagueoflegends.com/cdn/" + version + "/data/en_US/item.json");
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
                throw new IllegalStateException("GET " + url + " returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching " + url, e);
        }
    }
}