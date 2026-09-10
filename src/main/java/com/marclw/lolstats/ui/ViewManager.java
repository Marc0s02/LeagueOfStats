package com.marclw.lolstats.ui;

import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.prediction.PredictionService;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.ui.calculator.MainController;
import com.marclw.lolstats.ui.prediction.PredictionController;
import com.marclw.lolstats.ingest.ItemRepository;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Owns the primary Stage and swaps its Scene between the two top-level
 * views (Option A from the calculator/prediction pivot: same window,
 * content swaps out). Each screen's controller gets a back-reference to
 * this class so either one can ask to switch to the other.
 *
 * Also owns the shared services both views depend on (ChampionRepository,
 * StatCalculator, PredictionService), so neither controller has to
 * construct its own copies.
 */
public class ViewManager {

    private final Stage primaryStage;

    private final ChampionRepository championRepository;
    private final StatCalculator statCalculator;
    private final PredictionService predictionService;

    public ViewManager(Stage primaryStage,
                       ChampionRepository championRepository,
                       StatCalculator statCalculator,
                       PredictionService predictionService) {
        this.primaryStage = primaryStage;
        this.championRepository = championRepository;
        this.statCalculator = statCalculator;
        this.predictionService = predictionService;
    }

    public void showCalculatorView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/marclw/lolstats/ui/calculator/MainView.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setChampionRepository(championRepository);
            controller.setStatCalculator(statCalculator);
            controller.setViewManager(this);
            controller.setItemRepository(itemRepository);
            controller.populateData();

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("LeagueOfStats - Champion Comparison");
        } catch (IOException e) {
            // TODO: replace with real error handling/logging
            e.printStackTrace();
        }
    }

    public void showPredictionView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/marclw/lolstats/ui/prediction/PredictionView.fxml"));
            Parent root = loader.load();

            PredictionController controller = loader.getController();
            controller.setPredictionService(predictionService);
            controller.setViewManager(this);

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("LeagueOfStats - Match Prediction");
        } catch (IOException e) {
            // TODO: replace with real error handling/logging
            e.printStackTrace();
        }
    }

    private final ItemRepository itemRepository;

    public ViewManager(Stage primaryStage,
                       ChampionRepository championRepository,
                       ItemRepository itemRepository,
                       StatCalculator statCalculator,
                       PredictionService predictionService) {
        this.primaryStage = primaryStage;
        this.championRepository = championRepository;
        this.itemRepository = itemRepository;
        this.statCalculator = statCalculator;
        this.predictionService = predictionService;
    }
}
