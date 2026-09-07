package com.marclw.lolstats.ingest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Item;
import com.marclw.lolstats.model.Role;
import com.marclw.lolstats.model.Stats;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles local, gzip-compressed caching of champion/item data so repeat
 * launches don't need to hit the network. Stored outside the install
 * directory (e.g. under the user's home folder) so app size stays small.
 *
 * Cache files store OUR OWN Champion/Item model objects (Gson-serialized),
 * not raw Community Dragon JSON - all the CDragon-schema parsing happens
 * once, in refreshCache(), so loadCachedChampions()/loadCachedItems() on
 * every subsequent launch are simple and fast.
 */
public class CacheManager {

    private static final Type CHAMPION_LIST_TYPE = new TypeToken<List<Champion>>() {}.getType();
    private static final Type ITEM_LIST_TYPE = new TypeToken<List<Item>>() {}.getType();

    private final Gson gson = new Gson();
    private final ChampionDetailParser championDetailParser = new ChampionDetailParser();

    private Path cacheDir;
    private Path metadataFile;
    private String cachedPatchVersion;
    private CDragonClient client;

    public CacheManager() {
    }

    public CacheManager(Path cacheDir, CDragonClient client) {
        this.cacheDir = cacheDir;
        this.client = client;
        this.metadataFile = cacheDir.resolve("metadata.txt");
    }

    public List<Champion> loadCachedChampions() {
        return readGzipped(cacheDir.resolve("champions.json.gz"), CHAMPION_LIST_TYPE);
    }

    public List<Item> loadCachedItems() {
        return readGzipped(cacheDir.resolve("items.json.gz"), ITEM_LIST_TYPE);
    }

    /**
     * Compares cachedPatchVersion against CDragonClient.fetchLatestPatchVersion().
     * Also treats a missing/never-loaded cache as stale.
     */
    public boolean isCacheStale() {
        if (cachedPatchVersion == null) {
            loadCachedPatchVersion();
        }
        if (cachedPatchVersion == null || !Files.exists(cacheDir.resolve("champions.json.gz"))) {
            return true;
        }
        String latest = client.fetchLatestPatchVersion();
        return !latest.equals(cachedPatchVersion);
    }

    /**
     * Fetches fresh data via CDragonClient, gzip-compresses it, writes it to
     * cacheDir, and updates metadataFile with the new patch version.
     *
     * Note on cost: champion-summary.json is one request, but base stats
     * aren't in it - getting real stats for every champion means one
     * additional request PER champion (championDetailParser needs
     * champions/{id}.json). That's ~170 sequential HTTP calls on a cold
     * cache, which is genuinely slow (tens of seconds). Fine for now since
     * it only happens on a stale/missing cache, but worth parallelizing
     * later (e.g. an ExecutorService with a handful of threads) if it
     * becomes annoying during development.
     */
    public void refreshCache() {
        try {
            Files.createDirectories(cacheDir);

            List<Champion> champions = fetchAllChampions();
            List<Item> items = fetchAllItems();

            writeGzipped(cacheDir.resolve("champions.json.gz"), gson.toJson(champions, CHAMPION_LIST_TYPE));
            writeGzipped(cacheDir.resolve("items.json.gz"), gson.toJson(items, ITEM_LIST_TYPE));

            cachedPatchVersion = client.fetchLatestPatchVersion();
            Files.writeString(metadataFile, cachedPatchVersion, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to refresh cache at " + cacheDir, e);
        }
    }

    /**
     * Called on launch: checks isCacheStale() and calls refreshCache() only if needed.
     */
    public void checkForUpdates() {
        if (isCacheStale()) {
            refreshCache();
        }
    }

    private List<Champion> fetchAllChampions() {
        JsonArray summary = JsonParser.parseString(client.fetchChampionData()).getAsJsonArray();
        List<Champion> champions = new ArrayList<>();

        for (JsonElement element : summary) {
            JsonObject entry = element.getAsJsonObject();
            int id = entry.get("id").getAsInt();
            if (id <= 0) {
                continue; // id -1 is CDragon's "no champion" placeholder entry
            }
            String name = entry.get("name").getAsString();
            List<Role> roles = parseRoles(entry.get("roles").getAsJsonArray());

            String detailJson = client.fetchChampionDetail(id);
            Stats baseStats = championDetailParser.parseBaseStats(detailJson);
            Stats perLevelGrowth = championDetailParser.parsePerLevelGrowth(detailJson);

            champions.add(new Champion(id, name, roles, baseStats, perLevelGrowth));
        }
        return champions;
    }

    private List<Role> parseRoles(JsonArray rolesJson) {
        List<Role> roles = new ArrayList<>();
        for (JsonElement roleElement : rolesJson) {
            try {
                roles.add(Role.valueOf(roleElement.getAsString().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                // Unknown role string from the API - skip it rather than
                // fail the whole champion; e.g. if CDragon ever adds a
                // role your Role enum doesn't have yet.
            }
        }
        return roles;
    }
    
    private List<Item> fetchAllItems() {
        JsonArray itemsJson = JsonParser.parseString(client.fetchItemData()).getAsJsonArray();
        List<Item> items = new ArrayList<>();

        for (JsonElement element : itemsJson) {
            JsonObject entry = element.getAsJsonObject();
            Item item = new Item();
            item.setId(entry.get("id").getAsInt());
            item.setName(entry.get("name").getAsString());
            item.setCost(entry.has("priceTotal") ? entry.get("priceTotal").getAsInt() : 0);
            item.setInStore(entry.has("inStore") && entry.get("inStore").getAsBoolean());
            item.setRequiredAlly(entry.has("requiredAlly") ? entry.get("requiredAlly").getAsString() : "");
            item.setRequiredChampion(entry.has("requiredChampion") ? entry.get("requiredChampion").getAsString() : "");

            if (entry.has("maps") && entry.get("maps").isJsonObject()) {
                item.setMaps(gson.fromJson(entry.get("maps"), new TypeToken<java.util.Map<String, Boolean>>() {}.getType()));
            }

            JsonObject statsJson = entry.has("stats") && entry.get("stats").isJsonObject()
                    ? entry.getAsJsonObject("stats") : new JsonObject();
            item.setStatBonuses(new Stats(
                    itemStatValue(statsJson, "Health", "FlatHPPoolMod"),
                    itemStatValue(statsJson, "AttackDamage", "FlatPhysicalDamageMod"),
                    itemStatValue(statsJson, "AbilityPower", "FlatMagicDamageMod"),
                    itemStatValue(statsJson, "Armor", "FlatArmorMod"),
                    itemStatValue(statsJson, "MagicResistance", "FlatSpellBlockMod"),
                    itemStatValue(statsJson, "AttackSpeed", "PercentAttackSpeedMod"),
                    itemStatValue(statsJson, "MovementSpeed", "FlatMovementSpeedMod")
            ));

            items.add(item);
        }
        return items;
    }

    private double itemStatValue(JsonObject statsJson, String... possibleKeys) {
        for (String key : possibleKeys) {
            if (statsJson.has(key) && statsJson.get(key).isJsonPrimitive()) {
                return statsJson.get(key).getAsDouble();
            }
        }
        return 0.0;
    }

    private void loadCachedPatchVersion() {
        try {
            if (metadataFile != null && Files.exists(metadataFile)) {
                cachedPatchVersion = Files.readString(metadataFile, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            cachedPatchVersion = null;
        }
    }

    private void writeGzipped(Path path, String content) {
        try (Writer writer = new java.io.OutputStreamWriter(
                new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write cache file " + path, e);
        }
    }

    private <T> T readGzipped(Path path, Type type) {
        try (Reader reader = new java.io.InputStreamReader(
                new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read cache file " + path, e);
        }
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