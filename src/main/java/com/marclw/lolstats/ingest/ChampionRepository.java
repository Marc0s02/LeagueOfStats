package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Role;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChampionRepository {

    private List<Champion> champions;
    private CacheManager cacheManager;

    public ChampionRepository() {
    }

    public ChampionRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<Champion> loadAll() {
        cacheManager.checkForUpdates();
        champions = deduplicate(cacheManager.loadCachedChampions());
        return champions;
    }

    public Champion findByName(String name) {
        if (champions == null) loadAll();
        return champions.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Champion> filterByRole(Role role) {
        if (champions == null) loadAll();
        return champions.stream()
                .filter(c -> c.getRoles() != null && c.getRoles().contains(role))
                .toList();
    }

    public List<Champion> getChampions() {
        return champions == null ? Collections.emptyList() : champions;
    }

    public void setChampions(List<Champion> champions) {
        this.champions = deduplicate(champions);
    }

    private List<Champion> deduplicate(List<Champion> source) {
        if (source == null) return List.of();
        Map<Integer, Champion> unique = new LinkedHashMap<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Champion champion : source) {
            if (champion == null || champion.getId() <= 0) continue;
            String name = champion.getName() == null ? "" : champion.getName().trim().toLowerCase(Locale.ROOT);
            if (!unique.containsKey(champion.getId()) && (name.isEmpty() || names.add(name))) {
                unique.put(champion.getId(), champion);
            }
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(Champion::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public CacheManager getCacheManager() { return cacheManager; }
    public void setCacheManager(CacheManager cacheManager) { this.cacheManager = cacheManager; }
}
