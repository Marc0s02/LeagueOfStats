package com.marclw.lolstats.ui.advisor;

import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.ingest.ManualGameStateProvider;
import com.marclw.lolstats.ingest.RuneRepository;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Rune;
import com.marclw.lolstats.recommendation.BuildRecommender;
import com.marclw.lolstats.recommendation.GameState;
import com.marclw.lolstats.recommendation.HeuristicRuneRecommender;
import com.marclw.lolstats.recommendation.RecommendationSettings;
import com.marclw.lolstats.recommendation.RecommendedBuild;
import com.marclw.lolstats.recommendation.RecommendedRunePage;
import com.marclw.lolstats.recommendation.RuneRecommender;
import com.marclw.lolstats.ui.ViewManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wires up the recommendation screen: pick your champion + ally/enemy
 * comps, hit Get Recommendation, see a suggested build and rune page.
 *
 * Uses the heuristic path end to end (HeuristicBuildRecommender wrapped
 * by StatsAugmentedBuildRecommender, HeuristicRuneRecommender) - real
 * match-data annotation only activates if useRealMatchDataCheckBox is
 * checked AND MatchupStatsStore actually has data on disk (see
 * StatsAugmentedBuildRecommender - it degrades to pure-heuristic output
 * automatically if the toggle is on but there's nothing to annotate
 * with, rather than failing).
 *
 * RuneRecommender is constructed here, not injected via ViewManager, since
 * it needs the loaded rune list which isn't available until populateData()
 * runs - mirrors how MainController builds its own view state from
 * repositories rather than being handed fully-assembled objects.
 */
public class AdvisorController {

    private ChampionRepository championRepository;
    private RuneRepository runeRepository;
    private BuildRecommender buildRecommender;
    private RecommendationSettings recommendationSettings;
    private ViewManager viewManager;

    private RuneRecommender runeRecommender;
    private final ManualGameStateProvider gameStateProvider = new ManualGameStateProvider();

    @FXML private ComboBox<Champion> myChampionBox;
    @FXML private ListView<Champion> alliesListView;
    @FXML private ListView<Champion> enemiesListView;
    @FXML private CheckBox useRealMatchDataCheckBox;
    @FXML private Label statusLabel;
    @FXML private Label buildSummaryLabel;
    @FXML private ListView<String> itemRecommendationsListView;
    @FXML private Label keystoneLabel;
    @FXML private ListView<String> playstyleNotesListView;

    public void initialize() {
        alliesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        enemiesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        StringConverter<Champion> championDisplay = new StringConverter<>() {
            @Override
            public String toString(Champion champion) {
                return champion == null ? "" : champion.getName();
            }

            @Override
            public Champion fromString(String string) {
                return null;
            }
        };
        myChampionBox.setConverter(championDisplay);

        alliesListView.setCellFactory(list -> championCell());
        enemiesListView.setCellFactory(list -> championCell());

        statusLabel.setText("Pick your champion and the enemy team, then press Get Recommendation.");
    }

    private ListCell<Champion> championCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Champion champion, boolean empty) {
                super.updateItem(champion, empty);
                setText(empty || champion == null ? null : champion.getName());
            }
        };
    }

    /**
     * Called by ViewManager right after the repositories/recommender are
     * injected. Loads champions (reusing the cache MainController already
     * warmed, if this isn't the first screen visited) and runes (which is
     * genuinely new data - the calculator/prediction screens never touch
     * RuneRepository), then builds the RuneRecommender now that the rune
     * list actually exists.
     */
    public void populateData() {
        List<Champion> champions = championRepository.getChampions().isEmpty()
                ? championRepository.loadAll() : championRepository.getChampions();
        List<Rune> runes = runeRepository.getRunes().isEmpty()
                ? runeRepository.loadAll() : runeRepository.getRunes();

        runeRecommender = new HeuristicRuneRecommender(runes);

        ObservableList<Champion> championOptions = FXCollections.observableArrayList(champions);
        championOptions.sort(Comparator.comparing(Champion::getName));

        myChampionBox.setItems(championOptions);
        alliesListView.setItems(championOptions);
        enemiesListView.setItems(championOptions);

        if (!championOptions.isEmpty()) {
            myChampionBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void onGetRecommendation() {
        Champion myChampion = myChampionBox.getValue();
        if (myChampion == null) {
            statusLabel.setText("Select your champion first.");
            return;
        }

        gameStateProvider.setMyChampion(myChampion);
        gameStateProvider.setAllyChampions(new ArrayList<>(alliesListView.getSelectionModel().getSelectedItems()));
        gameStateProvider.setEnemyChampions(new ArrayList<>(enemiesListView.getSelectionModel().getSelectedItems()));
        recommendationSettings.setUseRealMatchData(useRealMatchDataCheckBox.isSelected());

        GameState gameState = gameStateProvider.getGameState();
        if (gameState == null) {
            statusLabel.setText("Select your champion first.");
            return;
        }

        RecommendedBuild build = buildRecommender.recommend(gameState);
        RecommendedRunePage runePage = runeRecommender.recommend(gameState);

        displayBuild(build);
        displayRunes(runePage);
        statusLabel.setText("Recommendation generated for " + myChampion.getName() + ".");
    }

    private void displayBuild(RecommendedBuild build) {
        buildSummaryLabel.setText(build.getSummary());

        ObservableList<String> itemLines = FXCollections.observableArrayList();
        if (build.getItems() != null) {
            for (RecommendedBuild.ItemRecommendation rec : build.getItems()) {
                StringBuilder line = new StringBuilder();
                line.append(rec.getItem().getName()).append(" - ").append(rec.getReasoning());
                // winRatePercent/gamesSampled are only non-null when the
                // real-match-data toggle is on AND MatchupStatsProvider had
                // enough sampled games (see MatchupStatsStore.MIN_SAMPLE_SIZE) -
                // null here means "no stats available", not 0%, so it's left
                // out entirely rather than shown as a misleading figure.
                if (rec.getWinRatePercent() != null) {
                    line.append(String.format(" [%.1f%% win rate, %d games]",
                            rec.getWinRatePercent(), rec.getGamesSampled()));
                }
                itemLines.add(line.toString());
            }
        }
        if (itemLines.isEmpty()) {
            itemLines.add("(no item recommendations for this comp yet)");
        }
        itemRecommendationsListView.setItems(itemLines);
    }

    private void displayRunes(RecommendedRunePage runePage) {
        keystoneLabel.setText(runePage.getKeystone() == null
                ? "Keystone: (rune data unavailable)"
                : "Keystone: " + runePage.getKeystone().getName());

        ObservableList<String> notes = FXCollections.observableArrayList();
        if (runePage.getPlaystyleNotes() != null) {
            notes.addAll(runePage.getPlaystyleNotes());
        }
        playstyleNotesListView.setItems(notes);
    }

    @FXML
    public void onSwitchToCalculator() {
        if (viewManager != null) {
            viewManager.showCalculatorView();
        }
    }

    @FXML
    public void onSwitchToPrediction() {
        if (viewManager != null) {
            viewManager.showPredictionView();
        }
    }

    public void setChampionRepository(ChampionRepository championRepository) {
        this.championRepository = championRepository;
    }

    public void setRuneRepository(RuneRepository runeRepository) {
        this.runeRepository = runeRepository;
    }

    public void setBuildRecommender(BuildRecommender buildRecommender) {
        this.buildRecommender = buildRecommender;
    }

    public void setRecommendationSettings(RecommendationSettings recommendationSettings) {
        this.recommendationSettings = recommendationSettings;
    }

    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }
}
