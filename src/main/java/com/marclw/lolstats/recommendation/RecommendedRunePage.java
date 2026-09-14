package com.marclw.lolstats.recommendation;

import com.marclw.lolstats.model.Rune;

import java.util.List;

/**
 * Output of a RuneRecommender. A real rune page is keystone + 3 more
 * primary-tree runes + 2 secondary-tree runes + 3 stat shards - modeled
 * here as flat lists rather than mirroring Data Dragon's slot structure,
 * since the recommender's job is "pick these specific runes", not
 * "here's a menu of choices per slot" (that structure lives in Rune/
 * DataDragonClient's runesReforged.json parsing instead).
 *
 * playstyleNotes is the "how this affects the way you should play"
 * half of the ask - short, concrete statements (e.g. "Electrocute wants
 * you picking short trades, not long ones") rather than a single wall of
 * text, so the UI can render them as a bullet list.
 */
public class RecommendedRunePage {

    private Rune keystone;
    private List<Rune> primaryTreeRunes; // the 3 non-keystone picks from the primary tree
    private List<Rune> secondaryTreeRunes; // the 2 picks from the secondary tree
    private List<String> statShards; // e.g. "Adaptive Force", "Armor" - see Rune.java note on shard data
    private List<String> playstyleNotes;

    public RecommendedRunePage() {
    }

    public Rune getKeystone() {
        return keystone;
    }

    public void setKeystone(Rune keystone) {
        this.keystone = keystone;
    }

    public List<Rune> getPrimaryTreeRunes() {
        return primaryTreeRunes;
    }

    public void setPrimaryTreeRunes(List<Rune> primaryTreeRunes) {
        this.primaryTreeRunes = primaryTreeRunes;
    }

    public List<Rune> getSecondaryTreeRunes() {
        return secondaryTreeRunes;
    }

    public void setSecondaryTreeRunes(List<Rune> secondaryTreeRunes) {
        this.secondaryTreeRunes = secondaryTreeRunes;
    }

    public List<String> getStatShards() {
        return statShards;
    }

    public void setStatShards(List<String> statShards) {
        this.statShards = statShards;
    }

    public List<String> getPlaystyleNotes() {
        return playstyleNotes;
    }

    public void setPlaystyleNotes(List<String> playstyleNotes) {
        this.playstyleNotes = playstyleNotes;
    }
}
