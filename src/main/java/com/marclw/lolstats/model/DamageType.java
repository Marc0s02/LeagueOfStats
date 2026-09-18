package com.marclw.lolstats.model;

/**
 * A champion's primary damage type, sourced from Community Dragon's
 * champions/{id}.json tacticalInfo.damageType field (values "kPhysical",
 * "kMagic", "kMixed" - see CacheManager.parseDamageType for the mapping).
 * UNKNOWN covers a missing/unrecognized value rather than defaulting to
 * PHYSICAL or MAGIC, so callers can distinguish "genuinely mixed/unusual"
 * from "we don't actually know" if that distinction ever matters.
 */
public enum DamageType {
    PHYSICAL,
    MAGIC,
    MIXED,
    UNKNOWN
}