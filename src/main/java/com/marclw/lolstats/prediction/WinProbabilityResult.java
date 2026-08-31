package com.marclw.lolstats.prediction;

/**
 * Output of a single prediction: blue side's win probability at the
 * minute the features were extracted at, plus enough context for the UI
 * to display it meaningfully.
 */
public class WinProbabilityResult {

    private String matchId;
    private int minute;
    private double blueWinProbability; // 0.0-1.0

    public WinProbabilityResult() {
    }

    public WinProbabilityResult(String matchId, int minute, double blueWinProbability) {
        this.matchId = matchId;
        this.minute = minute;
        this.blueWinProbability = blueWinProbability;
    }

    public double getRedWinProbability() {
        return 1.0 - blueWinProbability;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public double getBlueWinProbability() {
        return blueWinProbability;
    }

    public void setBlueWinProbability(double blueWinProbability) {
        this.blueWinProbability = blueWinProbability;
    }
}
