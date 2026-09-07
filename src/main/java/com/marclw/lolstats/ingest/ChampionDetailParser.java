package com.marclw.lolstats.ingest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.marclw.lolstats.model.Stats;

/**
 * Parses a single champions/{id}.json response into base Stats + per-level
 * growth Stats.
 *
 * Deliberately defensive: statValue() looks values up by name from a raw
 * JsonObject rather than binding fixed Gson DTO fields, so a wrong guess
 * about one key's exact spelling returns 0.0 for that one stat instead of
 * throwing and breaking every champion's parse.
 */
public class ChampionDetailParser {

    public Stats parseBaseStats(String championDetailJson) {
        JsonObject stats = statsBlock(championDetailJson);
        return new Stats(
                statValue(stats, "hp", "flat"),
                statValue(stats, "attackDamage", "flat"),
                0, // champions have no innate ability power stat
                statValue(stats, "armor", "flat"),
                statValue(stats, "magicResistance", "flat"),
                statValue(stats, "attackSpeed", "flat"),
                topLevelValue(championDetailJson, "movementSpeed")
        );
    }

    public Stats parsePerLevelGrowth(String championDetailJson) {
        JsonObject stats = statsBlock(championDetailJson);
        return new Stats(
                statValue(stats, "hp", "perLevel"),
                statValue(stats, "attackDamage", "perLevel"),
                0,
                statValue(stats, "armor", "perLevel"),
                statValue(stats, "magicResistance", "perLevel"),
                statValue(stats, "attackSpeed", "perLevel"),
                0 // move speed doesn't scale per level
        );
    }

    private JsonObject statsBlock(String championDetailJson) {
        JsonObject root = JsonParser.parseString(championDetailJson).getAsJsonObject();
        JsonElement stats = root.get("stats");
        return stats != null && stats.isJsonObject() ? stats.getAsJsonObject() : new JsonObject();
    }

    private double statValue(JsonObject stats, String statName, String subField) {
        JsonElement stat = stats.get(statName);
        if (stat == null || !stat.isJsonObject()) {
            return 0.0;
        }
        JsonElement value = stat.getAsJsonObject().get(subField);
        return value != null && value.isJsonPrimitive() ? value.getAsDouble() : 0.0;
    }

    private double topLevelValue(String championDetailJson, String fieldName) {
        JsonObject root = JsonParser.parseString(championDetailJson).getAsJsonObject();
        JsonElement value = root.get(fieldName);
        return value != null && value.isJsonPrimitive() ? value.getAsDouble() : 0.0;
    }
}