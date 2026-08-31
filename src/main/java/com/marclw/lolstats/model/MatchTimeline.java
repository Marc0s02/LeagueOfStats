package com.marclw.lolstats.model;

import java.util.List;

/**
 * A structured representation of a Riot Match-V5 "timeline" response -
 * per-minute frames of team/player state (gold, XP, objectives taken so
 * far). This is what makes "state at minute N" features possible, for
 * both training (finished games, any minute) and serving (a live game's
 * current minute).
 */
public class MatchTimeline {

    private String matchId;
    private List<Frame> frames; // one entry per in-game minute

    public MatchTimeline() {
    }

    /**
     * Snapshot of both teams' cumulative state at a single minute mark.
     */
    public static class Frame {
        private int minute;
        private int blueTotalGold;
        private int redTotalGold;
        private int blueTotalXp;
        private int redTotalXp;
        private int blueDragonKills;
        private int redDragonKills;
        private int blueHeraldKills;
        private int redHeraldKills;
        private int blueTowersDestroyed;
        private int redTowersDestroyed;

        public Frame() {
        }

        public int getMinute() {
            return minute;
        }

        public void setMinute(int minute) {
            this.minute = minute;
        }

        public int getBlueTotalGold() {
            return blueTotalGold;
        }

        public void setBlueTotalGold(int blueTotalGold) {
            this.blueTotalGold = blueTotalGold;
        }

        public int getRedTotalGold() {
            return redTotalGold;
        }

        public void setRedTotalGold(int redTotalGold) {
            this.redTotalGold = redTotalGold;
        }

        public int getBlueTotalXp() {
            return blueTotalXp;
        }

        public void setBlueTotalXp(int blueTotalXp) {
            this.blueTotalXp = blueTotalXp;
        }

        public int getRedTotalXp() {
            return redTotalXp;
        }

        public void setRedTotalXp(int redTotalXp) {
            this.redTotalXp = redTotalXp;
        }

        public int getBlueDragonKills() {
            return blueDragonKills;
        }

        public void setBlueDragonKills(int blueDragonKills) {
            this.blueDragonKills = blueDragonKills;
        }

        public int getRedDragonKills() {
            return redDragonKills;
        }

        public void setRedDragonKills(int redDragonKills) {
            this.redDragonKills = redDragonKills;
        }

        public int getBlueHeraldKills() {
            return blueHeraldKills;
        }

        public void setBlueHeraldKills(int blueHeraldKills) {
            this.blueHeraldKills = blueHeraldKills;
        }

        public int getRedHeraldKills() {
            return redHeraldKills;
        }

        public void setRedHeraldKills(int redHeraldKills) {
            this.redHeraldKills = redHeraldKills;
        }

        public int getBlueTowersDestroyed() {
            return blueTowersDestroyed;
        }

        public void setBlueTowersDestroyed(int blueTowersDestroyed) {
            this.blueTowersDestroyed = blueTowersDestroyed;
        }

        public int getRedTowersDestroyed() {
            return redTowersDestroyed;
        }

        public void setRedTowersDestroyed(int redTowersDestroyed) {
            this.redTowersDestroyed = redTowersDestroyed;
        }
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public void setFrames(List<Frame> frames) {
        this.frames = frames;
    }

    /**
     * Convenience lookup used by FeatureExtractor to grab the frame closest
     * to (but not after) a requested minute - e.g. "state at 15 minutes".
     *
     * Deliberately "latest frame at or before minute" rather than requiring
     * an exact match: a live game's current minute won't always land
     * exactly on a frame boundary, and a finished game might be shorter
     * than the requested minute (e.g. asking for minute 30 on a 22-minute
     * game) - in that case this returns the last available frame rather
     * than null, since "state at game end" is still a meaningful answer.
     * Returns null only if there are no frames at or before the minute at
     * all (e.g. an empty timeline, or minute 0 with no frame yet).
     */
    public Frame getFrameAtMinute(int minute) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        Frame best = null;
        for (Frame frame : frames) {
            if (frame.getMinute() <= minute) {
                if (best == null || frame.getMinute() > best.getMinute()) {
                    best = frame;
                }
            }
        }
        return best;
    }
}
