package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Role;

import java.util.Collections;
import java.util.List;

public class ChampionRepository {

    private List<Champion> champions;
    private CacheManager cacheManager;

    public ChampionRepository() {
    }

    public ChampionRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Ensures the cache is fresh (fetching if needed), loads it, and caches
     * the result in-memory for the rest of this call - findByName()/
     * filterByRole() work against that in-memory list rather than re-reading
     * from disk each time.
     */
    public List<Champion> loadAll() {
        cacheManager.checkForUpdates();
        champions = cacheManager.loadCachedChampions();
        return champions;
    }

    public Champion findByName(String name) {
        if (champions == null) {
            loadAll();
        }
        return champions.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Champion> filterByRole(Role role) {
        if (champions == null) {
            loadAll();
        }
        return champions.stream()
                .filter(c -> c.getRoles() != null && c.getRoles().contains(role))
                .toList();
    }

    public List<Champion> getChampions() {
        return champions == null ? Collections.emptyList() : champions;
    }

    public void setChampions(List<Champion> champions) {
        this.champions = champions;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}