package com.marclw.lolstats.ui.calculator;

import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.ui.ViewManager;

/**
 * Wires up the main screen: champion TableView, role/mode filters,
 * level + build selectors, and the compare/chart panels.
 *
 * Drop MainView.fxml (and the @FXML annotations) if you'd rather build the
 * scene graph in code instead - either approach works fine.
 */
public class MainController {

    private ChampionRepository championRepository;
    private StatCalculator statCalculator;

    // Set by ViewManager right after this controller is loaded, so this
    // screen can ask to switch to the prediction view.
    private ViewManager viewManager;

    public void initialize() {
    }

    /**
     * Called by the "Switch to Match Prediction" button (see MainView.fxml,
     * onAction="#onSwitchToPrediction"). Hands control back to ViewManager,
     * which swaps the Stage's Scene over to PredictionView.
     */
    public void onSwitchToPrediction() {
        if (viewManager != null) {
            viewManager.showPredictionView();
        }
    }

    public ChampionRepository getChampionRepository() {
        return championRepository;
    }

    public void setChampionRepository(ChampionRepository championRepository) {
        this.championRepository = championRepository;
    }

    public StatCalculator getStatCalculator() {
        return statCalculator;
    }

    public void setStatCalculator(StatCalculator statCalculator) {
        this.statCalculator = statCalculator;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }

    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }
}
