package com.marclw.lolstats.features;

import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;

/**
 * Rolls per-player stats and per-minute timeline frames up into
 * per-team totals and blue-minus-red diffs (gold, XP, kills, objectives).
 * Kept separate from FeatureExtractor so the "sum these numbers per team"
 * logic can be unit-tested independently of feature-naming/ordering
 * concerns.
 */
public class TeamAggregator {

    public int goldDiffAtMinute(MatchTimeline timeline, int minute) {
        return 0;
    }

    public int xpDiffAtMinute(MatchTimeline timeline, int minute) {
        return 0;
    }

    public int dragonDiffAtMinute(MatchTimeline timeline, int minute) {
        return 0;
    }

    public int heraldDiffAtMinute(MatchTimeline timeline, int minute) {
        return 0;
    }

    public int towerDiffAtMinute(MatchTimeline timeline, int minute) {
        return 0;
    }

    /**
     * Kill diff isn't in MatchTimeline.Frame directly (only final
     * MatchRecord has a clean per-player kill count) - this likely needs
     * to derive kills-so-far from timeline event data instead, once that's
     * added to MatchTimeline. Left as a clear TODO rather than guessed at.
     */
    public int killDiffAtMinute(MatchRecord match, MatchTimeline timeline, int minute) {
        return 0;
    }
}
