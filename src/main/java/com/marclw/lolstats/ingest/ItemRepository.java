package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Item;

import java.util.Collections;
import java.util.List;

/**
 * Mirrors ChampionRepository, but for items - kept as a separate class
 * rather than folding item-loading into ChampionRepository, since "champion
 * repository" shouldn't own item data conceptually, even though both
 * currently sit on top of the same CacheManager.
 */
public class ItemRepository {

    private List<Item> items;
    private CacheManager cacheManager;

    public ItemRepository() {
    }

    public ItemRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<Item> loadAll() {
        cacheManager.checkForUpdates();
        items = cacheManager.loadCachedItems();
        return items;
    }

    public List<Item> getItems() {
        return items == null ? Collections.emptyList() : items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}