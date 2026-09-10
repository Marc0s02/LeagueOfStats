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

    // All diffs are blue-minus-red, matching FeatureVector's sign convention
    // (positive = blue ahead) - keep that convention consistent if more
    // diff methods are added later, since FeatureExtractor doesn't flip
    // signs itself.

    public int goldDiffAtMinute(MatchTimeline timeline, int minute) {
        MatchTimeline.Frame frame = timeline.getFrameAtMinute(minute);
        if (frame == null) {
            return 0;
        }
        return frame.getBlueTotalGold() - frame.getRedTotalGold();
    }

    public int xpDiffAtMinute(MatchTimeline timeline, int minute) {
        MatchTimeline.Frame frame = timeline.getFrameAtMinute(minute);
        if (frame == null) {
            return 0;
        }
        return frame.getBlueTotalXp() - frame.getRedTotalXp();
    }

    public int dragonDiffAtMinute(MatchTimeline timeline, int minute) {
        MatchTimeline.Frame frame = timeline.getFrameAtMinute(minute);
        if (frame == null) {
            return 0;
        }
        return frame.getBlueDragonKills() - frame.getRedDragonKills();
    }

    public int heraldDiffAtMinute(MatchTimeline timeline, int minute) {
        MatchTimeline.Frame frame = timeline.getFrameAtMinute(minute);
        if (frame == null) {
            return 0;
        }
        return frame.getBlueHeraldKills() - frame.getRedHeraldKills();
    }

    public int towerDiffAtMinute(MatchTimeline timeline, int minute) {
        MatchTimeline.Frame frame = timeline.getFrameAtMinute(minute);
        if (frame == null) {
            return 0;
        }
        return frame.getBlueTowersDestroyed() - frame.getRedTowersDestroyed();
    }

    /**
     * NOT YET IMPLEMENTED - kill count isn't in MatchTimeline.Frame at all
     * (it only tracks gold/XP/objectives), and MatchRecord only has each
     * player's FINAL kill count, not kills-so-far at an arbitrary minute.
     * Getting this right needs one of:
     *   (a) Riot's timeline "CHAMPION_KILL" events (a per-frame events
     *       list Match-V5 timelines actually include, not yet modeled on
     *       Frame here), summed up to the requested minute, or
     *   (b) a simpler proxy if (a) proves too fiddly for the project's
     *       time budget - e.g. skip kill diff as a feature entirely and
     *       rely on gold/XP diff, which already captures most of the same
     *       signal (kills grant gold).
     * Left returning 0 (rather than guessing) so this doesn't silently
     * masquerade as working - FeatureExtractor.killDiffAtMinute will need
     * revisiting alongside whichever option is picked.
     */
    /**
     * Now backed by real data: MatchDataParser accumulates CHAMPION_KILL
     * events per team into Frame.blueChampionKills/redChampionKills, resolved
     * via a participantId -> team map (see MatchDataParser.buildParticipantTeamMap),
     * not the "1-5 is blue" assumption. The match parameter is unused now that
     * kills live on the Frame itself, but kept for signature stability with
     * FeatureExtractor's existing call site.
     */
    public int killDiffAtMinute(MatchRecord match, MatchTimeline timeline, int minute) {
        MatchTimeline.Frame frame = timeline.getFrameAtMinute(minute);
        if (frame == null) {
            return 0;
        }
        return frame.getBlueChampionKills() - frame.getRedChampionKills();
    }
}
