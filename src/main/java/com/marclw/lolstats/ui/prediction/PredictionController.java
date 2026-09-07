package com.marclw.lolstats.ui.prediction;

import com.marclw.lolstats.prediction.PredictionService;
import com.marclw.lolstats.ui.ViewManager;

/**
 * Wires up the live/match-prediction screen: match-ID input (or live-game
 * selector), a "Predict" action that calls PredictionService, and a
 * win-probability result display.
 */
public class PredictionController {

    private PredictionService predictionService;

    // Set by ViewManager right after this controller is loaded, so this
    // screen can ask to switch back to the calculator view.
    private ViewManager viewManager;

    public void initialize() {
    }

    /**
     * Called once the user has entered a match ID / picked a live game.
     * TODO: fetch -> extract features -> score -> display WinProbabilityResult.
     */
    public void onPredictButtonClicked() {
    }

    /**
     * Called by the "Back to Stat Calculator" button (see PredictionView.fxml,
     * onAction="#onSwitchToCalculator").
     */
    public void onSwitchToCalculator() {
        if (viewManager != null) {
            viewManager.showCalculatorView();
        }
    }

    public PredictionService getPredictionService() {
        return predictionService;
    }

    public void setPredictionService(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }

    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }
}