package com.marclw.lolstats.data;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Item;

import java.nio.file.Path;
import java.util.List;

/**
 * Handles local, gzip-compressed caching of champion/item data so repeat
 * launches don't need to hit the network. Stored outside the install
 * directory (e.g. under the user's home folder) so app size stays small.
 *
 * Suggested location: System.getProperty("user.home") + "/.lolstats/"
 * Suggested compression: java.util.zip.GZIPOutputStream / GZIPInputStream
 */
public class CacheManager {

    private Path cacheDir;
    private Path metadataFile;
    private String cachedPatchVersion;
    private CDragonClient client;

    public CacheManager() {
    }

    public CacheManager(Path cacheDir, CDragonClient client) {
        this.cacheDir = cacheDir;
        this.client = client;
    }

    public List<Champion> loadCachedChampions() {
        return null;
    }

    public List<Item> loadCachedItems() {
        return null;
    }

    /**
     * Compares cachedPatchVersion against CDragonClient.fetchLatestPatchVersion().
     */
    public boolean isCacheStale() {
        return false;
    }

    /**
     * Fetches fresh data via CDragonClient, gzip-compresses it, writes it to
     * cacheDir, and updates metadataFile with the new patch version.
     */
    public void refreshCache() {
    }

    /**
     * Called on launch: checks isCacheStale() and calls refreshCache() only if needed.
     */
    public void checkForUpdates() {
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public Path getMetadataFile() {
        return metadataFile;
    }

    public void setMetadataFile(Path metadataFile) {
        this.metadataFile = metadataFile;
    }

    public String getCachedPatchVersion() {
        return cachedPatchVersion;
    }

    public void setCachedPatchVersion(String cachedPatchVersion) {
        this.cachedPatchVersion = cachedPatchVersion;
    }
}
