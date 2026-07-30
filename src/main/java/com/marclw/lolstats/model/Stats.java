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

    public double getTotal() {
        return 0;
    }

    /**
     * Returns a new Stats combining this and other (e.g. base stats + item bonuses).
     */
    public Stats add(Stats other) {
        return null;
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
