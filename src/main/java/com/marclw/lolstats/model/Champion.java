package com.marclw.lolstats.model;

import java.util.List;

public class Champion {

    private int id;
    private String name;
    private List<Role> roles;
    private Stats baseStats;

    public Champion() {
    }

    public Champion(int id, String name, List<Role> roles, Stats baseStats) {
        this.id = id;
        this.name = name;
        this.roles = roles;
        this.baseStats = baseStats;
    }

    /**
     * Applies Riot's per-level growth formula to this champion's base stats.
     * statAtLevel = base + perLevel * (level - 1) * (0.7025 + 0.0175 * (level - 1))
     */
    public Stats getStatsAtLevel(int level) {
        return null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public Stats getBaseStats() {
        return baseStats;
    }

    public void setBaseStats(Stats baseStats) {
        this.baseStats = baseStats;
    }
}
