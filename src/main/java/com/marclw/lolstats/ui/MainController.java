package com.marclw.lolstats.ui;

import com.marclw.lolstats.data.ChampionRepository;
import com.marclw.lolstats.service.StatCalculator;

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

    public void initialize() {
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
}
