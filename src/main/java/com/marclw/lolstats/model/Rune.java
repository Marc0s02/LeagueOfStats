package com.marclw.lolstats.model;

/**
 * A single rune (keystone or minor slot rune). Source data is Data
 * Dragon's runesReforged.json (cdn/{version}/data/en_US/runesReforged.json)
 * - a separate endpoint from champion.json/item.json, not yet wired into
 * DataDragonClient/CacheManager. That file is structured as a list of
 * rune trees (Precision, Domination, Sorcery, Resolve, Inspiration), each
 * containing "slots", each slot containing a list of Rune-shaped entries.
 *
 * treeId/treeName are denormalized onto each Rune (rather than only
 * living on a parent tree object) so a flat List<Rune> is enough for
 * simple lookups (e.g. "find rune by id") without always needing the
 * tree structure at hand.
 */
public class Rune {

    private int id;
    private String key;
    private String name;
    private String shortDescription;
    private int treeId;
    private String treeName; // e.g. "Precision", "Domination"
    private boolean keystone; // true only for slot-0 runes in each tree

    public Rune() {
    }

    public Rune(int id, String key, String name, String shortDescription,
                int treeId, String treeName, boolean keystone) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.shortDescription = shortDescription;
        this.treeId = treeId;
        this.treeName = treeName;
        this.keystone = keystone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(int treeId) {
        this.treeId = treeId;
    }

    public String getTreeName() {
        return treeName;
    }

    public void setTreeName(String treeName) {
        this.treeName = treeName;
    }

    public boolean isKeystone() {
        return keystone;
    }

    public void setKeystone(boolean keystone) {
        this.keystone = keystone;
    }
}
