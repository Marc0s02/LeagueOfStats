package com.marclw.lolstats.service;

import com.marclw.lolstats.model.Build;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Stats;

public class StatCalculator {

    public Stats calculateFinalStats(Champion champion, int level, Build build) {
        return champion.getStatsAtLevel(level).add(build.getTotalStats());
    }

    /**
     * Simplified auto-attack damage: raw attack damage, no target armor
     * mitigation applied (this method only receives the attacker's Stats,
     * not a target's). Real LoL damage mitigation is
     * damage * 100 / (100 + targetArmor) - once the UI supports picking a
     * target champion for comparison, that belongs in a method that takes
     * both attacker and target Stats, not here.
     */
    public double calculateAutoAttackDamage(Stats stats) {
        return stats.getAttackDamage();
    }

    /**
     * Effective HP against physical damage only: hp * (1 + armor / 100).
     * This is the standard LoL formula for "how much raw physical damage
     * this unit can absorb before dying" - e.g. 2000 HP with 100 armor
     * behaves like 4000 HP against pure physical damage. It intentionally
     * ignores magic resist; a champion's *overall* effective health depends
     * on the attacker's damage mix (physical vs magic vs true), which this
     * single-target-agnostic method can't know. A more complete version
     * would take a damage-type ratio (or a specific enemy composition) as
     * a second parameter.
     */
    public double calculateEffectiveHealth(Stats stats) {
        return stats.getHp() * (1 + stats.getArmor() / 100.0);
    }
}
