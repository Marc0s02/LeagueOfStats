package com.marclw.lolstats.ingest;

import com.google.gson.JsonObject;
import com.marclw.lolstats.model.Stats;

/**
 * Parses ONE champion's entry from Data Dragon's champion.json "data"
 * object into base Stats + per-level growth Stats.
 *
 * Schema (verified, stable, officially documented by Riot):
 *   "stats": {
 *     "hp": ..., "hpperlevel": ...,
 *     "armor": ..., "armorperlevel": ...,
 *     "spellblock": ..., "spellblockperlevel": ...,   (magic resist)
 *     "attackdamage": ..., "attackdamageperlevel": ...,
 *     "movespeed": ...,
 *     "attackspeedoffset": ..., "attackspeedperlevel": ...
 *   }
 *
 */
public class ChampionDetailParser {

    public Stats parseBaseStats(JsonObject statsJson) {
        return new Stats(
                value(statsJson, "hp"),
                value(statsJson, "attackdamage"),
                0,
                value(statsJson, "armor"),
                value(statsJson, "spellblock"),
                value(statsJson, "attackspeed"),   // base AS is now given directly
                value(statsJson, "movespeed")
        );
    }

    public Stats parsePerLevelGrowth(JsonObject statsJson) {
        double baseAttackSpeed = value(statsJson, "attackspeed");
        double approxAttackSpeedPerLevel = baseAttackSpeed * (value(statsJson, "attackspeedperlevel") / 100.0);
        return new Stats(
                value(statsJson, "hpperlevel"),
                value(statsJson, "attackdamageperlevel"),
                0,
                value(statsJson, "armorperlevel"),
                value(statsJson, "spellblockperlevel"),
                approxAttackSpeedPerLevel,
                0
        );
    }

    private double value(JsonObject statsJson, String fieldName) {
        return statsJson.has(fieldName) ? statsJson.get(fieldName).getAsDouble() : 0.0;
    }
}