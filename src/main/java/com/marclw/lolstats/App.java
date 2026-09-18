package com.marclw.lolstats;

import com.marclw.lolstats.features.FeatureExtractor;
import com.marclw.lolstats.features.TeamAggregator;
import com.marclw.lolstats.ingest.CDragonClient;
import com.marclw.lolstats.ingest.CacheManager;
import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.ingest.ItemRepository;
import com.marclw.lolstats.ingest.MatchDataParser;
import com.marclw.lolstats.ingest.RiotMatchClient;
import com.marclw.lolstats.ingest.RuneRepository;
import com.marclw.lolstats.prediction.PredictionService;
import com.marclw.lolstats.recommendation.BuildRecommender;
import com.marclw.lolstats.recommendation.HeuristicBuildRecommender;
import com.marclw.lolstats.recommendation.RecommendationSettings;
import com.marclw.lolstats.recommendation.StatsAugmentedBuildRecommender;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.stats.MatchupStatsStore;
import com.marclw.lolstats.storage.ModelRegistry;
import com.marclw.lolstats.ui.ViewManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {

    /**
     * Regional routing for Match-V5 (europe/americas/asia/sea) - NOT the
     * platform host (euw1/na1). Override with -Dlolstats.region=... if you
     * play outside Europe.
     */
    private static final String DEFAULT_REGIONAL_BASE_URL = "https://europe.api.riotgames.com";

    @Override
    public void start(Stage primaryStage) {
        // --- Shared services, constructed once and handed to both views ---

        CDragonClient cDragonClient = new CDragonClient(
                "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/");
        CacheManager cacheManager = new CacheManager(
                Path.of(System.getProperty("user.home"), ".lolstats"), cDragonClient);
        ChampionRepository championRepository = new ChampionRepository(cacheManager);
        ItemRepository itemRepository = new ItemRepository(cacheManager);
        RuneRepository runeRepository = new RuneRepository(cacheManager);
        StatCalculator statCalculator = new StatCalculator();

        // --- Prediction wiring -------------------------------------------
        // This FeatureExtractor must match the one TrainingPipelineRunner
        // trains with (same TeamAggregator/StatCalculator). Both construct
        // it the same way on purpose - if you change one, change both, or
        // serving features stop meaning what the model was fitted on.
        FeatureExtractor featureExtractor =
                new FeatureExtractor(new TeamAggregator(), statCalculator);

        String apiKey = System.getenv("RIOT_API_KEY");
        String regionalBaseUrl = System.getProperty("lolstats.region", DEFAULT_REGIONAL_BASE_URL);
        Path dataDirectory = Path.of(System.getProperty("lolstats.data", "data"));

        RiotMatchClient matchClient = new RiotMatchClient(apiKey, regionalBaseUrl);
        ModelRegistry modelRegistry = new ModelRegistry(dataDirectory.resolve("model"));
        PredictionService predictionService = new PredictionService(
                modelRegistry, matchClient, new MatchDataParser(), featureExtractor);

        if (apiKey == null || apiKey.isBlank()) {
            // Not fatal: the champion-comparison view works entirely offline
            // from the CommunityDragon cache and doesn't need a Riot key.
            // Only the prediction view will fail, and it reports why itself.
            System.out.println("RIOT_API_KEY not set - match prediction will be unavailable. "
                    + "Champion comparison still works.");
        }

        // --- Advisor wiring ------------------------------------------------
        // MatchupStatsStore.load() is safe to call even when nothing has
        // ever been written to dataDirectory/stats - readGzipped returns
        // null for a missing file and load() turns that into empty maps,
        // so mostPopularItemIdsForMatchup/winRateForMatchup simply return
        // null/empty until an offline stats-building run actually produces
        // that data. The advisor screen works with pure heuristics either way.
        MatchupStatsStore matchupStatsStore = new MatchupStatsStore(dataDirectory.resolve("stats"));
        matchupStatsStore.load();

        RecommendationSettings recommendationSettings = new RecommendationSettings();
        BuildRecommender baseBuildRecommender = new HeuristicBuildRecommender(itemRepository);
        BuildRecommender buildRecommender = new StatsAugmentedBuildRecommender(
                baseBuildRecommender, matchupStatsStore, recommendationSettings);

        ViewManager viewManager = new ViewManager(
                primaryStage, championRepository, itemRepository, runeRepository,
                statCalculator, predictionService, buildRecommender, recommendationSettings);
        viewManager.showCalculatorView();

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}