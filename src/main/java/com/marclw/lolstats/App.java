package com.marclw.lolstats;

import com.marclw.lolstats.ingest.CDragonClient;
import com.marclw.lolstats.ingest.CacheManager;
import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.prediction.PredictionService;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.storage.ModelRegistry;
import com.marclw.lolstats.ui.ViewManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // --- Shared services, constructed once and handed to both views ---

        CDragonClient cDragonClient = new CDragonClient(
                "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/");
        CacheManager cacheManager = new CacheManager(
                Path.of(System.getProperty("user.home"), ".lolstats"), cDragonClient);
        ChampionRepository championRepository = new ChampionRepository(cacheManager);
        StatCalculator statCalculator = new StatCalculator();

        // TODO: construct real RiotMatchClient/RiotSpectatorClient + ModelRegistry
        // once credentials/config are in place; PredictionService needs both.
        ModelRegistry modelRegistry = new ModelRegistry();
        PredictionService predictionService = new PredictionService(modelRegistry);

        // TODO: call cacheManager.checkForUpdates() here (or on a background
        // thread) before the calculator view needs champion/item data.

        ViewManager viewManager = new ViewManager(
                primaryStage, championRepository, statCalculator, predictionService);
        viewManager.showCalculatorView();

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
