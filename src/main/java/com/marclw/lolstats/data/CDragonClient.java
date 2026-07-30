package com.marclw.lolstats.data;

/**
 * Talks to Community Dragon (raw.communitydragon.org) to fetch raw champion,
 * item, and patch-version data.
 *
 * Base URL: https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/
 */
public class CDragonClient {

    private String baseUrl;

    public CDragonClient() {
    }

    public CDragonClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Fetches champion-summary.json + champions/[id].json data (base stats, per-level growth).
     */
    public String fetchChampionData() {
        return null;
    }

    /**
     * Fetches items.json (stat bonuses, cost, build paths, inStore/maps/requiredAlly fields).
     */
    public String fetchItemData() {
        return null;
    }

    /**
     * Used by CacheManager to check whether the local cache is stale.
     */
    public String fetchLatestPatchVersion() {
        return null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
