package com.marclw.lolstats.ui;

import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.ingest.ItemRepository;
import com.marclw.lolstats.ingest.RuneRepository;
import com.marclw.lolstats.prediction.PredictionService;
import com.marclw.lolstats.recommendation.BuildRecommender;
import com.marclw.lolstats.recommendation.RecommendationSettings;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.ui.advisor.AdvisorController;
import com.marclw.lolstats.ui.calculator.MainController;
import com.marclw.lolstats.ui.prediction.PredictionController;
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
 * ItemRepository, StatCalculator, PredictionService), so neither controller
 * has to construct its own copies.
 */
public class ViewManager {

    private final Stage primaryStage;

    private final ChampionRepository championRepository;
    private final ItemRepository itemRepository;
    private final RuneRepository runeRepository;
    private final StatCalculator statCalculator;
    private final PredictionService predictionService;
    private final BuildRecommender buildRecommender;
    private final RecommendationSettings recommendationSettings;

    public ViewManager(Stage primaryStage,
                       ChampionRepository championRepository,
                       ItemRepository itemRepository,
                       RuneRepository runeRepository,
                       StatCalculator statCalculator,
                       PredictionService predictionService,
                       BuildRecommender buildRecommender,
                       RecommendationSettings recommendationSettings) {
        this.primaryStage = primaryStage;
        this.championRepository = championRepository;
        this.itemRepository = itemRepository;
        this.runeRepository = runeRepository;
        this.statCalculator = statCalculator;
        this.predictionService = predictionService;
        this.buildRecommender = buildRecommender;
        this.recommendationSettings = recommendationSettings;
    }

    public void showCalculatorView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/marclw/lolstats/ui/calculator/MainView.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setChampionRepository(championRepository);
            controller.setItemRepository(itemRepository);
            controller.setStatCalculator(statCalculator);
            controller.setViewManager(this);
            controller.populateData();

            primaryStage.setScene(createScene(root));
            primaryStage.setTitle("LeagueOfStats - Summoner's Rift Analyzer");
            primaryStage.setMinWidth(1180);
            primaryStage.setMinHeight(820);
            primaryStage.setWidth(Math.max(primaryStage.getWidth(), 1380));
            primaryStage.setHeight(Math.max(primaryStage.getHeight(), 900));
        } catch (IOException | RuntimeException e) {
            // TODO: replace with real error handling/logging
            e.printStackTrace();
        }
    }

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root);
        var stylesheet = getClass().getResource("/com/marclw/lolstats/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    public void showPredictionView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/marclw/lolstats/ui/prediction/PredictionView.fxml"));
            Parent root = loader.load();

            PredictionController controller = loader.getController();
            controller.setPredictionService(predictionService);
            controller.setViewManager(this);

            primaryStage.setScene(createScene(root));
            primaryStage.setTitle("LeagueOfStats - Match Prediction");
        } catch (IOException | RuntimeException e) {
            // TODO: replace with real error handling/logging
            e.printStackTrace();
        }
    }

    public void showAdvisorView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/marclw/lolstats/ui/advisor/AdvisorView.fxml"));
            Parent root = loader.load();

            AdvisorController controller = loader.getController();
            controller.setChampionRepository(championRepository);
            controller.setRuneRepository(runeRepository);
            controller.setBuildRecommender(buildRecommender);
            controller.setRecommendationSettings(recommendationSettings);
            controller.setViewManager(this);
            controller.populateData();

            primaryStage.setScene(createScene(root));
            primaryStage.setTitle("LeagueOfStats - Build & Rune Advisor");
        } catch (IOException | RuntimeException e) {
            // TODO: replace with real error handling/logging
            e.printStackTrace();
        }
    }
}