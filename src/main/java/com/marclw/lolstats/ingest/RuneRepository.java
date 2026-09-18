package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Rune;

import java.util.Collections;
import java.util.List;

/**
 * Mirrors ChampionRepository/ItemRepository's shape - loadAll()/getRunes()
 * backed by CacheManager. This is what finally unblocks
 * HeuristicRuneRecommender, which was written against exactly this
 * List<Rune> shape from the start and needs no logic changes to use it.
 */
public class RuneRepository {

    private List<Rune> runes;
    private CacheManager cacheManager;

    public RuneRepository() {
    }

    public RuneRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<Rune> loadAll() {
        cacheManager.checkForUpdates();
        runes = cacheManager.loadCachedRunes();
        return runes;
    }

    public List<Rune> getRunes() {
        return runes == null ? Collections.emptyList() : runes;
    }

    public void setRunes(List<Rune> runes) {
        this.runes = runes;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}