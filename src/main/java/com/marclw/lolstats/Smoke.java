package com.marclw.lolstats;

import com.marclw.lolstats.ingest.CDragonClient;
import com.marclw.lolstats.ingest.CacheManager;
import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Item;
import com.marclw.lolstats.model.Stats;

import java.nio.file.Path;
import java.util.List;

/**
 * THROWAWAY - not part of the app, just a manual verification harness for
 * the ingest pipeline (CDragonClient -> CacheManager -> ChampionRepository).
 * Delete this once MainController actually wires up ChampionRepository and
 * you don't need a standalone way to check it anymore.
 *
 */
public class Smoke {

    public static void main(String[] args) {
        CDragonClient cDragonClient = new CDragonClient(
                "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/");
        CacheManager cacheManager = new CacheManager(
                Path.of(System.getProperty("user.home"), ".lolstats"), cDragonClient);
        ChampionRepository championRepository = new ChampionRepository(cacheManager);

        // --- TEMPORARY: find the real "stats" field names ---
        String json = cDragonClient.fetchChampionDetail(266);
        com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        System.out.println("--- TOP-LEVEL KEYS ---");
        System.out.println(root.keySet());
        System.out.println("--- END ---");
        // --- end temporary ---

        System.out.println("Fetching/loading champions - first run will be slow "
                + "(one HTTP request per champion, ~170 of them)...");
        long start = System.currentTimeMillis();

        List<Champion> champions = championRepository.loadAll();

        long elapsedMs = System.currentTimeMillis() - start;
        System.out.println("Loaded " + champions.size() + " champions in " + elapsedMs + "ms");

        // Spot-check a well-known champion so the numbers are easy to verify
        // by eye against the League Wiki - Aatrox, id 266.
        Champion aatrox = champions.stream()
                .filter(c -> c.getId() == 266)
                .findFirst()
                .orElse(null);

        if (aatrox == null) {
            System.out.println("!! Could not find Aatrox (id 266) in the loaded list - "
                    + "something's wrong before we even get to stat parsing.");
        } else {
            Stats base = aatrox.getBaseStats();
            Stats perLevel = aatrox.getPerLevelGrowth();
            System.out.println("Aatrox base stats:      " + describe(base));
            System.out.println("Aatrox per-level growth: " + describe(perLevel));
            System.out.println("Expected (League Wiki, roughly): hp=650 armor=38 attackDamage=60 "
                    + "movespeed=345 hpPerLevel=114 armorPerLevel~4.4 adPerLevel=5");
            if (base != null && base.getHp() == 0) {
                System.out.println("!! All zeros suggests ChampionDetailParser's field-name guesses "
                        + "are wrong - see the verification comment in that class.");
            }
        }

        System.out.println();
        System.out.println("Fetching/loading items...");
        List<Item> items = cacheManager.loadCachedItems();
        System.out.println("Loaded " + items.size() + " items");

        Item infinityEdge = items.stream()
                .filter(i -> i.getId() == 3031)
                .findFirst()
                .orElse(null);
        if (infinityEdge == null) {
            System.out.println("!! Could not find Infinity Edge (id 3031) in the loaded list.");
        } else {
            System.out.println("Infinity Edge stat bonuses: " + describe(infinityEdge.getStatBonuses()));
            System.out.println("Expected (roughly): attackDamage=70, crit not modeled in our Stats class");
        }

        System.out.println();
        System.out.println("Cache directory: " + cacheManager.getCacheDir());
        System.out.println("Cached patch version: " + cacheManager.getCachedPatchVersion());
    }

    private static String describe(Stats stats) {
        if (stats == null) {
            return "null";
        }
        return String.format(
                "hp=%.1f ad=%.1f ap=%.1f armor=%.1f mr=%.1f as=%.3f ms=%.1f",
                stats.getHp(), stats.getAttackDamage(), stats.getAbilityPower(),
                stats.getArmor(), stats.getMagicResist(), stats.getAttackSpeed(), stats.getMoveSpeed());
    }
}