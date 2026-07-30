package com.marclw.lolstats.data;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Role;

import java.util.List;

public class ChampionRepository {

    private List<Champion> champions;
    private CacheManager cacheManager;

    public ChampionRepository() {
    }

    public ChampionRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<Champion> loadAll() {
        return null;
    }

    public Champion findByName(String name) {
        return null;
    }

    public List<Champion> filterByRole(Role role) {
        return null;
    }

    public List<Champion> getChampions() {
        return champions;
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
