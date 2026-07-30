package com.marclw.lolstats.model;

import java.util.List;

public class Champion {

    private int id;
    private String name;
    private List<Role> roles;
    private Stats baseStats;
    private Stats perLevelGrowth;

    public Champion() {
    }

    public Champion(int id, String name, List<Role> roles, Stats baseStats, Stats perLevelGrowth) {
        this.id = id;
        this.name = name;
        this.roles = roles;
        this.baseStats = baseStats;
        this.perLevelGrowth = perLevelGrowth;
    }

    /**
     * Applies Riot's per-level growth formula to this champion's base stats.
     * statAtLevel = base + perLevel * (level - 1) * (0.7025 + 0.0175 * (level - 1))
     */

    public Stats getPerLevelGrowth(){
        return perLevelGrowth;
    }

    public void setPerLevelGrowth(Stats perLevelGrowth){
        this.perLevelGrowth = perLevelGrowth;
    }

    public Stats getStatsAtLevel(int level) {
        double multiplier = (level -1) * (0.7025 + 0.0175 * (level - 1));
        double newHp = baseStats.getHp() + (perLevelGrowth.getHp() * multiplier);
        double newAttackDamage = baseStats.getAttackDamage() + (perLevelGrowth.getAttackDamage() * multiplier);
        double newAbilityPower = baseStats.getAbilityPower() + (perLevelGrowth.getAbilityPower() * multiplier);
        double newArmor = baseStats.getArmor() + (perLevelGrowth.getArmor() * multiplier);
        double newMagicResist = baseStats.getMagicResist() + (perLevelGrowth.getMagicResist() * multiplier);
        double newAttackSpeed = baseStats.getAttackSpeed() + (perLevelGrowth.getAttackSpeed() * multiplier);
        double newMoveSpeed = baseStats.getMoveSpeed() + (perLevelGrowth.getMoveSpeed() * multiplier);
        return new Stats(newHp, newAttackDamage, newAbilityPower, newArmor, newMagicResist, newAttackSpeed, newMoveSpeed);

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
