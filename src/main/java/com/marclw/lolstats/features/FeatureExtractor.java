package com.marclw.lolstats.features;

import com.marclw.lolstats.model.FeatureVector;
import com.marclw.lolstats.model.MatchRecord;
import com.marclw.lolstats.model.MatchTimeline;
import com.marclw.lolstats.service.StatCalculator;

/**
 * The single piece of logic both the offline (training) and online
 * (serving) paths depend on: given a match's data and a minute mark,
 * produce a FeatureVector.
 *
 * This is deliberately the ONLY place feature values get computed.
 * training.DatasetBuilder calls this once per historical match (with the
 * known outcome available, so it also sets FeatureVector.blueTeamWon).
 * prediction.PredictionService calls this once per live/recent match
 * request (outcome unknown, blueTeamWon left unset). Keeping both paths
 * calling through here - rather than each having its own copy of "how do
 * I compute goldDiffAtMinute" - is what prevents training/serving skew.
 *
 * Feature names/order come from FeatureSpec, not hardcoded here, so
 * adding a feature means updating FeatureSpec + the corresponding
 * calculation, not touching every caller.
 */
public class FeatureExtractor {

    private final TeamAggregator teamAggregator;
    private final StatCalculator statCalculator;

    public FeatureExtractor(TeamAggregator teamAggregator, StatCalculator statCalculator) {
        this.teamAggregator = teamAggregator;
        this.statCalculator = statCalculator;
    }

    /**
     * @param match   final/known match data (may be partial/unknown-outcome
     *                for a live game, depending on how PredictionService
     *                assembles it)
     * @param timeline per-minute frames for this match
     * @param minute  the point in the game to compute features "as of" -
     *                for training this can be any minute from a finished
     *                game; for serving it's the live game's current minute
     */
    public FeatureVector extract(MatchRecord match, MatchTimeline timeline, int minute) {
        FeatureVector vector = new FeatureVector();

        vector.put("goldDiffAtMinute", teamAggregator.goldDiffAtMinute(timeline, minute));
        vector.put("xpDiffAtMinute", teamAggregator.xpDiffAtMinute(timeline, minute));
        vector.put("killDiffAtMinute", teamAggregator.killDiffAtMinute(match, timeline, minute));
        vector.put("dragonDiffAtMinute", teamAggregator.dragonDiffAtMinute(timeline, minute));
        vector.put("heraldDiffAtMinute", teamAggregator.heraldDiffAtMinute(timeline, minute));
        vector.put("towerDiffAtMinute", teamAggregator.towerDiffAtMinute(timeline, minute));

        // TODO: champion win-rate lookups (needs a static win-rate reference
        // table, e.g. scraped/cached from a stats site or computed from the
        // training corpus itself) and composition engage-score (needs a
        // simple per-champion "engage potential" rating, hand-authored or
        // derived from Role/Stats). Left unset (0.0 default) until those
        // reference sources are decided on.

        // TODO: this is the natural place to fold in StatCalculator -
        // e.g. an "effective power at this level/minute" feature computed
        // from each team's champions' base stats via
        // statCalculator.calculateFinalStats(...). That's how the original
        // stat-comparison logic feeds into prediction rather than being
        // dropped.

        return vector;
    }
}
