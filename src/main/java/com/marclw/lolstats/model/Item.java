package com.marclw.lolstats.model;

import java.util.Map;

public class Item {

    private int id;
    private String name;
    private int cost;
    private Stats statBonuses;

    // Community Dragon fields used for filtering out unavailable / restricted items
    private boolean inStore;
    private Map<String, Boolean> maps;
    private String requiredAlly;
    private String requiredChampion;

    public Item() {
    }

    public Stats getStatBonuses() {
        return statBonuses;
    }

    public void setStatBonuses(Stats statBonuses) {
        this.statBonuses = statBonuses;
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

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public boolean isInStore() {
        return inStore;
    }

    public void setInStore(boolean inStore) {
        this.inStore = inStore;
    }

    public Map<String, Boolean> getMaps() {
        return maps;
    }

    public void setMaps(Map<String, Boolean> maps) {
        this.maps = maps;
    }

    public String getRequiredAlly() {
        return requiredAlly;
    }

    public void setRequiredAlly(String requiredAlly) {
        this.requiredAlly = requiredAlly;
    }

    public String getRequiredChampion() {
        return requiredChampion;
    }

    public void setRequiredChampion(String requiredChampion) {
        this.requiredChampion = requiredChampion;
    }
}
