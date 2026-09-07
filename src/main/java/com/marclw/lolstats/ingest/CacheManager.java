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

    /**
     * Bump this whenever the CDragon/Data Dragon parsing logic changes
     * (field-name fixes, new stats being read, etc.) - it's independent of
     * the League patch version, and forces isCacheStale() to refresh even
     * when the game patch hasn't moved. Without this, a cache written by a
     * buggy build of the parser would sit on disk forever, silently masking
     * any later fix, since isCacheStale() otherwise only compares patch
     * strings.
     */
    private static final int CACHE_FORMAT_VERSION = 2;

    private final Gson gson = new Gson();
    private final ChampionDetailParser championDetailParser = new ChampionDetailParser();

    private Path cacheDir;
    private Path metadataFile;
    private String cachedPatchVersion;
    private int cachedFormatVersion = -1;
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
     * Compares cachedPatchVersion against CDragonClient.fetchLatestPatchVersion(),
     * and cachedFormatVersion against CACHE_FORMAT_VERSION. Also treats a
     * missing/never-loaded cache as stale.
     *
     * The format-version check matters even when the patch hasn't changed:
     * it's what forces a rewrite after a parsing-logic fix, instead of
     * silently continuing to serve whatever (possibly wrong) values were
     * cached under the old code.
     */
    public boolean isCacheStale() {
        if (cachedPatchVersion == null) {
            loadCachedMetadata();
        }
        if (cachedPatchVersion == null || !Files.exists(cacheDir.resolve("champions.json.gz"))) {
            return true;
        }
        if (cachedFormatVersion != CACHE_FORMAT_VERSION) {
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
            cachedFormatVersion = CACHE_FORMAT_VERSION;
            Files.writeString(metadataFile,
                    cachedFormatVersion + "\n" + cachedPatchVersion,
                    StandardCharsets.UTF_8);
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

        String version = dataDragonClient.fetchLatestVersion();
        JsonObject ddragonChampions = JsonParser.parseString(dataDragonClient.fetchChampionData(version))
                .getAsJsonObject().getAsJsonObject("data");

        // Data Dragon keys its "data" object by champion NAME (e.g. "Aatrox"),
        // not numeric id - build a lookup by numeric "key" instead, since
        // that's what champion-summary.json's "id" field matches.
        java.util.Map<Integer, JsonObject> statsByChampionId = new java.util.HashMap<>();
        for (String ddragonName : ddragonChampions.keySet()) {
            JsonObject entry = ddragonChampions.getAsJsonObject(ddragonName);
            int key = entry.get("key").getAsInt();
            statsByChampionId.put(key, entry.getAsJsonObject("stats"));
        }

        List<Champion> champions = new ArrayList<>();
        for (JsonElement element : summary) {
            JsonObject entry = element.getAsJsonObject();
            int id = entry.get("id").getAsInt();
            if (id <= 0) {
                continue;
            }
            String name = entry.get("name").getAsString();
            List<Role> roles = parseRoles(entry.get("roles").getAsJsonArray());

            JsonObject statsJson = statsByChampionId.get(id);
            Stats baseStats;
            Stats perLevelGrowth;
            if (statsJson != null) {
                baseStats = championDetailParser.parseBaseStats(statsJson);
                perLevelGrowth = championDetailParser.parsePerLevelGrowth(statsJson);
            } else {
                // Happens if a champion exists on CDragon (e.g. very recently
                // released) before Data Dragon has caught up - fall back to
                // zeroed stats rather than failing the whole load.
                baseStats = new Stats(0, 0, 0, 0, 0, 0, 0);
                perLevelGrowth = new Stats(0, 0, 0, 0, 0, 0, 0);
            }

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
        String version = dataDragonClient.fetchLatestVersion();
        JsonObject ddragonItems = JsonParser.parseString(dataDragonClient.fetchItemData(version))
                .getAsJsonObject().getAsJsonObject("data");

        List<Item> items = new ArrayList<>();
        for (String idString : ddragonItems.keySet()) {
            JsonObject entry = ddragonItems.getAsJsonObject(idString);
            Item item = new Item();
            item.setId(Integer.parseInt(idString));
            item.setName(entry.get("name").getAsString());

            if (entry.has("gold") && entry.getAsJsonObject("gold").has("total")) {
                item.setCost(entry.getAsJsonObject("gold").get("total").getAsInt());
            }

            JsonObject statsJson = entry.has("stats") && entry.get("stats").isJsonObject()
                    ? entry.getAsJsonObject("stats") : new JsonObject();
            item.setStatBonuses(new Stats(
                    itemStatValue(statsJson, "FlatHPPoolMod"),
                    itemStatValue(statsJson, "FlatPhysicalDamageMod"),
                    itemStatValue(statsJson, "FlatMagicDamageMod"),
                    itemStatValue(statsJson, "FlatArmorMod"),
                    itemStatValue(statsJson, "FlatSpellBlockMod"),
                    itemStatValue(statsJson, "PercentAttackSpeedMod"),
                    itemStatValue(statsJson, "FlatMovementSpeedMod")
            ));

            if (entry.has("maps")) {
                item.setMaps(gson.fromJson(entry.get("maps"), new TypeToken<java.util.Map<String, Boolean>>() {}.getType()));
            }
            item.setInStore(!entry.has("inStore") || entry.get("inStore").getAsBoolean());
            item.setRequiredChampion(entry.has("requiredChampion") ? entry.get("requiredChampion").getAsString() : "");
            item.setRequiredAlly(entry.has("requiredAlly") ? entry.get("requiredAlly").getAsString() : "");

            items.add(item);
        }
        return items;
    }

    private double itemStatValue(JsonObject statsJson, String key) {
        return statsJson.has(key) ? statsJson.get(key).getAsDouble() : 0.0;
    }

    private double itemStatValue(JsonObject statsJson, String... possibleKeys) {
        for (String key : possibleKeys) {
            if (statsJson.has(key) && statsJson.get(key).isJsonPrimitive()) {
                return statsJson.get(key).getAsDouble();
            }
        }
        return 0.0;
    }

    /**
     * metadata.txt is two lines: the CACHE_FORMAT_VERSION the cache was
     * written under, then the League patch version. Older caches written
     * before this format existed have just the patch string on line one -
     * that's treated as format version -1 (never matches CACHE_FORMAT_VERSION),
     * so isCacheStale() correctly forces a refresh instead of misreading the
     * patch string as a format version or crashing on the parse.
     */
    private void loadCachedMetadata() {
        try {
            if (metadataFile == null || !Files.exists(metadataFile)) {
                cachedPatchVersion = null;
                cachedFormatVersion = -1;
                return;
            }
            List<String> lines = Files.readAllLines(metadataFile, StandardCharsets.UTF_8);
            if (lines.size() >= 2) {
                cachedFormatVersion = parseFormatVersion(lines.get(0));
                cachedPatchVersion = lines.get(1).trim();
            } else if (lines.size() == 1) {
                // Pre-existing single-line metadata.txt from before the
                // format-version field was added.
                cachedFormatVersion = -1;
                cachedPatchVersion = lines.get(0).trim();
            } else {
                cachedFormatVersion = -1;
                cachedPatchVersion = null;
            }
        } catch (IOException e) {
            cachedPatchVersion = null;
            cachedFormatVersion = -1;
        }
    }

    private int parseFormatVersion(String line) {
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            return -1;
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

    private final DataDragonClient dataDragonClient = new DataDragonClient();
}