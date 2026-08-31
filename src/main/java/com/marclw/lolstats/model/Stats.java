package com.marclw.lolstats.model;

public class Stats {

    private double hp;
    private double attackDamage;
    private double abilityPower;
    private double armor;
    private double magicResist;
    private double attackSpeed;
    private double moveSpeed;

    public Stats() {
    }

    public Stats(double hp, double attackDamage, double abilityPower, double armor,
                 double magicResist, double attackSpeed, double moveSpeed) {
        this.hp = hp;
        this.attackDamage = attackDamage;
        this.abilityPower = abilityPower;
        this.armor = armor;
        this.magicResist = magicResist;
        this.attackSpeed = attackSpeed;
        this.moveSpeed = moveSpeed;
    }

    /**
     * Naive sum of every stat field. Because these fields are on wildly
     * different scales (hp is in the hundreds/thousands, moveSpeed ~300-500,
     * attackSpeed is a small fraction), this is NOT a meaningful "power
     * score" on its own - it exists as a quick, cheap sort key for the
     * TableView (e.g. "total stat points"), not as anything to feed into
     * FeatureExtractor or present as a real power comparison. If the UI
     * needs an actual weighted power rating later, that should be a
     * separate method with explicit per-stat weights, not this one.
     */
    public double getTotal() {
        return hp + attackDamage + abilityPower + armor + magicResist + attackSpeed + moveSpeed;
    }

    /**
     * Returns a new Stats combining this and other (e.g. base stats + item bonuses).
     */
    public Stats add(Stats other) {
        return new Stats(
                this.hp + other.hp,
                this.attackDamage + other.attackDamage,
                this.abilityPower + other.abilityPower,
                this.armor + other.armor,
                this.magicResist + other.magicResist,
                this.attackSpeed + other.attackSpeed,
                this.moveSpeed + other.moveSpeed
        );
    }

    public double getHp() {
        return hp;
    }

    public void setHp(double hp) {
        this.hp = hp;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    public double getAbilityPower() {
        return abilityPower;
    }

    public void setAbilityPower(double abilityPower) {
        this.abilityPower = abilityPower;
    }

    public double getArmor() {
        return armor;
    }

    public void setArmor(double armor) {
        this.armor = armor;
    }

    public double getMagicResist() {
        return magicResist;
    }

    public void setMagicResist(double magicResist) {
        this.magicResist = magicResist;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(double moveSpeed) {
        this.moveSpeed = moveSpeed;
    }
}
