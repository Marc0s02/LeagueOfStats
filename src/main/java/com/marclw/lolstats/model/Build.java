package com.marclw.lolstats.model;

import java.util.ArrayList;
import java.util.List;

public class Build {

    private List<Item> items = new ArrayList<>();

    public void addItem(Item item) {
    }

    public Stats getTotalStats() {
        return null;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
