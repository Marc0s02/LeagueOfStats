package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.GameMode;
import com.marclw.lolstats.model.Item;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        items = deduplicate(cacheManager.loadCachedItems());
        return items;
    }

    public List<Item> getItems() {
        return items == null ? Collections.emptyList() : items;
    }

    /** Returns only items valid for the current calculator mode/champion. */
    public List<Item> getSummonersRiftItems(Champion champion) {
        if (items == null || items.isEmpty()) loadAll();
        Map<String, Item> uniqueByName = new LinkedHashMap<>();
        for (Item item : items) {
            if (!ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, champion)) continue;
            String key = item.getName() == null ? String.valueOf(item.getId())
                    : item.getName().trim().toLowerCase(Locale.ROOT);
            uniqueByName.putIfAbsent(key, item);
        }
        return uniqueByName.values().stream()
                .sorted(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void setItems(List<Item> items) {
        this.items = deduplicate(items);
    }

    private List<Item> deduplicate(List<Item> source) {
        if (source == null) return List.of();
        Map<Integer, Item> unique = new LinkedHashMap<>();
        for (Item item : source) {
            if (item == null || item.getId() <= 0) continue;
            unique.putIfAbsent(item.getId(), item);
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public CacheManager getCacheManager() { return cacheManager; }
    public void setCacheManager(CacheManager cacheManager) { this.cacheManager = cacheManager; }
}
