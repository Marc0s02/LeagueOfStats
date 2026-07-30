package com.marclw.lolstats.service;

import com.marclw.lolstats.model.Build;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Stats;

public class StatCalculator {

    /**
     * champion.getStatsAtLevel(level).add(build.getTotalStats())
     */
    public Stats calculateFinalStats(Champion champion, int level, Build build) {
        return null;
    }

    public double calculateAutoAttackDamage(Stats stats) {
        return 0;
    }

    public double calculateEffectiveHealth(Stats stats) {
        return 0;
    }
}
