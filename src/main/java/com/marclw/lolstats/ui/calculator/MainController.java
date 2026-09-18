package com.marclw.lolstats.ui.calculator;

import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.ingest.ItemRepository;
import com.marclw.lolstats.model.Build;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Item;
import com.marclw.lolstats.model.Stats;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.ui.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Main Summoner's Rift calculator screen.
 *
 * The UI is deliberately stateful: champions are added to visible 5-player
 * teams and each selected champion keeps its own level/build while the user
 * moves around the team. This replaces the old hidden Ctrl/Cmd multi-select
 * ListView interaction.
 */
public class MainController {

    private static final int TEAM_SIZE = 5;
    private static final int BUILD_SIZE = 6;

    private ChampionRepository championRepository;
    private ItemRepository itemRepository;
    private StatCalculator statCalculator;
    private ViewManager viewManager;

    @FXML private FlowPane redTeamSlots;
    @FXML private FlowPane blueTeamSlots;
    @FXML private FlowPane championGrid;
    @FXML private FlowPane itemGrid;
    @FXML private FlowPane buildSlots;
    @FXML private TextField championSearchField;
    @FXML private TextField itemSearchField;
    @FXML private Label activeTeamLabel;
    @FXML private Label selectedChampionNameLabel;
    @FXML private Label selectedChampionMetaLabel;
    @FXML private ImageView selectedChampionImage;
    @FXML private Spinner<Integer> selectedLevelSpinner;
    @FXML private Label selectedStatsLabel;
    @FXML private Label buildSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private Button redTeamButton;
    @FXML private Button blueTeamButton;

    private final List<Champion> allChampions = new ArrayList<>();
    private final List<Item> allSummonersRiftItems = new ArrayList<>();
    private final List<Champion> redTeam = new ArrayList<>();
    private final List<Champion> blueTeam = new ArrayList<>();
    private final Map<Integer, List<Item>> buildsByChampionId = new HashMap<>();
    private final Map<Integer, Integer> levelsByChampionId = new HashMap<>();

    private String activeTeam = "RED";
    private Champion selectedChampion;

    public void initialize() {
        selectedLevelSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 18, 1));

        championSearchField.textProperty().addListener((obs, oldValue, newValue) -> refreshChampionGrid());
        itemSearchField.textProperty().addListener((obs, oldValue, newValue) -> refreshItemGrid());
        selectedLevelSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (selectedChampion != null && newValue != null) {
                levelsByChampionId.put(selectedChampion.getId(), newValue);
                refreshSelectedChampion();
            }
        });

        setActiveTeam("RED");
        statusLabel.setText("Select a champion to add to the Red Team.");
    }

    public void populateData() {
        List<Champion> champions = championRepository.getChampions().isEmpty()
                ? championRepository.loadAll() : championRepository.getChampions();
        List<Item> items = itemRepository.getSummonersRiftItems(null);

        allChampions.clear();
        allChampions.addAll(champions.stream()
                .filter(c -> c != null && c.getId() > 0 && c.getName() != null && !c.getName().isBlank())
                .collect(Collectors.toMap(
                        Champion::getId,
                        c -> c,
                        (first, ignored) -> first))
                .values());
        allChampions.sort(Comparator.comparing(Champion::getName, String.CASE_INSENSITIVE_ORDER));

        allSummonersRiftItems.clear();
        allSummonersRiftItems.addAll(items);
        allSummonersRiftItems.sort(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER));

        refreshChampionGrid();
        refreshTeamSlots();
        statusLabel.setText("Summoner's Rift • Ranked / Normal • " + allChampions.size() + " champions loaded");
    }

    @FXML
    public void onSelectRedTeam() {
        setActiveTeam("RED");
    }

    @FXML
    public void onSelectBlueTeam() {
        setActiveTeam("BLUE");
    }

    private void setActiveTeam(String team) {
        activeTeam = team;
        activeTeamLabel.setText("Adding champions to " + ("RED".equals(team) ? "Red Team" : "Blue Team"));
        redTeamButton.getStyleClass().remove("team-toggle-active");
        blueTeamButton.getStyleClass().remove("team-toggle-active");
        ("RED".equals(team) ? redTeamButton : blueTeamButton).getStyleClass().add("team-toggle-active");
        refreshChampionGrid();
    }

    private void refreshChampionGrid() {
        if (championGrid == null) return;
        championGrid.getChildren().clear();
        String query = championSearchField == null ? "" : championSearchField.getText().trim().toLowerCase(Locale.ROOT);

        for (Champion champion : allChampions) {
            if (!query.isEmpty() && !champion.getName().toLowerCase().contains(query)) {
                continue;
            }
            championGrid.getChildren().add(createChampionPickerCard(champion));
        }
    }

    private Button createChampionPickerCard(Champion champion) {
        Button button = new Button();
        button.getStyleClass().add("champion-picker-card");
        button.setMinSize(92, 112);
        button.setPrefSize(92, 112);
        button.setMaxSize(92, 112);

        VBox content = new VBox(6);
        content.setAlignment(Pos.CENTER);
        ImageView image = imageView(champion.getIconUrl(), 58, 58);
        Label name = new Label(champion.getName());
        name.getStyleClass().add("champion-picker-name");
        name.setWrapText(true);
        name.setMaxWidth(82);
        name.setAlignment(Pos.CENTER);
        content.getChildren().addAll(image, name);
        button.setGraphic(content);

        boolean alreadyOnAnyTeam = containsChampion(redTeam, champion) || containsChampion(blueTeam, champion);
        if (alreadyOnAnyTeam) {
            button.getStyleClass().add("champion-picker-selected");
            button.setDisable(true);
        }

        button.setOnAction(event -> addChampionToActiveTeam(champion));
        return button;
    }

    private void addChampionToActiveTeam(Champion champion) {
        List<Champion> team = "RED".equals(activeTeam) ? redTeam : blueTeam;
        if (team.size() >= TEAM_SIZE) {
            statusLabel.setText(("RED".equals(activeTeam) ? "Red" : "Blue") + " Team is already full.");
            return;
        }
        if (containsChampion(redTeam, champion) || containsChampion(blueTeam, champion)) {
            statusLabel.setText(champion.getName() + " is already on a team.");
            return;
        }

        team.add(champion);
        buildsByChampionId.putIfAbsent(champion.getId(), new ArrayList<>());
        levelsByChampionId.putIfAbsent(champion.getId(), 1);
        selectedChampion = champion;
        refreshTeamSlots();
        refreshChampionGrid();
        refreshSelectedChampion();
        statusLabel.setText(champion.getName() + " added to " + ("RED".equals(activeTeam) ? "Red" : "Blue") + " Team.");
    }

    private void refreshTeamSlots() {
        renderTeam(redTeamSlots, redTeam, "RED");
        renderTeam(blueTeamSlots, blueTeam, "BLUE");
    }

    private void renderTeam(FlowPane target, List<Champion> team, String teamName) {
        target.getChildren().clear();
        for (int i = 0; i < TEAM_SIZE; i++) {
            if (i < team.size()) {
                target.getChildren().add(createTeamChampionCard(team.get(i), teamName));
            } else {
                target.getChildren().add(createEmptyTeamSlot(teamName));
            }
        }
    }

    private VBox createTeamChampionCard(Champion champion, String teamName) {
        VBox card = new VBox(5);
        card.getStyleClass().add("team-champion-card");
        if (selectedChampion != null && selectedChampion.getId() == champion.getId()) {
            card.getStyleClass().add("team-champion-card-active");
        }
        card.setPrefSize(118, 132);
        card.setAlignment(Pos.CENTER);

        Button select = new Button();
        select.getStyleClass().add("team-champion-image-button");
        select.setGraphic(imageView(champion.getIconUrl(), 72, 72));
        select.setOnAction(e -> {
            selectedChampion = champion;
            refreshTeamSlots();
            refreshSelectedChampion();
        });

        Label name = new Label(champion.getName());
        name.getStyleClass().add("team-champion-name");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);

        Button remove = new Button("×");
        remove.getStyleClass().add("remove-champion-button");
        remove.setOnAction(e -> removeChampion(champion));

        HBox footer = new HBox(4, name, remove);
        footer.setAlignment(Pos.CENTER);
        card.getChildren().addAll(select, footer);
        return card;
    }

    private VBox createEmptyTeamSlot(String teamName) {
        VBox slot = new VBox();
        slot.getStyleClass().add("team-empty-slot");
        slot.setPrefSize(118, 132);
        slot.setAlignment(Pos.CENTER);
        Label plus = new Label("+");
        plus.getStyleClass().add("empty-slot-plus");
        Label text = new Label("Select Champion");
        text.getStyleClass().add("empty-slot-text");
        slot.getChildren().addAll(plus, text);
        slot.setOnMouseClicked(e -> setActiveTeam(teamName));
        return slot;
    }

    private void removeChampion(Champion champion) {
        redTeam.removeIf(c -> c.getId() == champion.getId());
        blueTeam.removeIf(c -> c.getId() == champion.getId());
        if (selectedChampion != null && selectedChampion.getId() == champion.getId()) {
            selectedChampion = null;
        }
        refreshTeamSlots();
        refreshChampionGrid();
        refreshSelectedChampion();
        statusLabel.setText(champion.getName() + " removed from the team.");
    }

    private void refreshSelectedChampion() {
        if (selectedChampion == null) {
            selectedChampionNameLabel.setText("No champion selected");
            selectedChampionMetaLabel.setText("Click a team champion or add one from the selector.");
            selectedChampionImage.setImage(null);
            selectedStatsLabel.setText("—");
            buildSummaryLabel.setText("0 / 6 items");
            buildSlots.getChildren().clear();
            itemGrid.getChildren().clear();
            return;
        }

        selectedChampionNameLabel.setText(selectedChampion.getName());
        selectedChampionMetaLabel.setText("Level " + levelsByChampionId.getOrDefault(selectedChampion.getId(), 1)
                + " • " + ("RED".equals(teamOf(selectedChampion)) ? "Red Team" : "Blue Team"));
        selectedChampionImage.setImage(loadImage(selectedChampion.getIconUrl()));

        int level = levelsByChampionId.getOrDefault(selectedChampion.getId(), 1);
        selectedLevelSpinner.getValueFactory().setValue(level);
        refreshBuildSlots();
        refreshItemGrid();
        updateSelectedStats();
    }

    private void refreshBuildSlots() {
        buildSlots.getChildren().clear();
        List<Item> build = currentBuild();
        for (int i = 0; i < BUILD_SIZE; i++) {
            StackPane slot = new StackPane();
            slot.getStyleClass().add("build-slot");
            if (i < build.size()) {
                Item item = build.get(i);
                ImageView icon = imageView(item.getIconUrl(), 48, 48);
                slot.getChildren().add(icon);
                Label index = new Label(String.valueOf(i + 1));
                index.getStyleClass().add("build-slot-index");
                StackPane.setAlignment(index, Pos.TOP_LEFT);
                slot.getChildren().add(index);
                slot.setOnMouseClicked(e -> removeItemFromBuild(item));
                javafx.scene.control.Tooltip.install(slot, new javafx.scene.control.Tooltip(item.getName() + "\nClick to remove"));
            } else {
                Label plus = new Label("+");
                plus.getStyleClass().add("build-slot-plus");
                slot.getChildren().add(plus);
            }
            buildSlots.getChildren().add(slot);
        }
        buildSummaryLabel.setText(build.size() + " / " + BUILD_SIZE + " items • Click an item to add • Click a build slot to remove");
    }

    private void refreshItemGrid() {
        if (itemGrid == null) return;
        itemGrid.getChildren().clear();
        if (selectedChampion == null) return;

        String query = itemSearchField == null ? "" : itemSearchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Item> items = itemRepository.getSummonersRiftItems(selectedChampion);
        for (Item item : items) {
            if (!query.isEmpty() && !item.getName().toLowerCase().contains(query)) {
                continue;
            }
            itemGrid.getChildren().add(createItemCard(item));
        }
    }

    private Button createItemCard(Item item) {
        Button button = new Button();
        button.getStyleClass().add("item-card");
        button.setMinSize(76, 88);
        button.setPrefSize(76, 88);
        button.setMaxSize(76, 88);

        VBox content = new VBox(4);
        content.setAlignment(Pos.CENTER);
        content.getChildren().add(imageView(item.getIconUrl(), 42, 42));
        Label name = new Label(item.getName());
        name.getStyleClass().add("item-card-name");
        name.setWrapText(true);
        name.setMaxWidth(68);
        name.setAlignment(Pos.CENTER);
        content.getChildren().add(name);
        button.setGraphic(content);

        if (currentBuild().stream().anyMatch(i -> i.getId() == item.getId())) {
            button.getStyleClass().add("item-card-selected");
        }
        button.setOnAction(e -> toggleItem(item));
        return button;
    }

    private void toggleItem(Item item) {
        if (selectedChampion == null) return;
        List<Item> build = currentBuild();
        int existingIndex = findItemIndex(build, item);
        if (existingIndex >= 0) {
            build.remove(existingIndex);
            statusLabel.setText(item.getName() + " removed from " + selectedChampion.getName() + "'s build.");
        } else if (build.size() >= BUILD_SIZE) {
            statusLabel.setText("Build is full. Remove an item before adding another.");
        } else {
            build.add(item);
            statusLabel.setText(item.getName() + " added to " + selectedChampion.getName() + "'s build.");
        }
        refreshBuildSlots();
        refreshItemGrid();
        updateSelectedStats();
    }

    private void removeItemFromBuild(Item item) {
        if (selectedChampion == null) return;
        currentBuild().removeIf(i -> i.getId() == item.getId());
        refreshBuildSlots();
        refreshItemGrid();
        updateSelectedStats();
    }

    private void updateSelectedStats() {
        if (selectedChampion == null) return;
        int level = levelsByChampionId.getOrDefault(selectedChampion.getId(), 1);
        Build build = new Build();
        currentBuild().forEach(build::addItem);
        Stats stats = statCalculator.calculateFinalStats(selectedChampion, level, build);
        selectedStatsLabel.setText(describe(stats));
    }

    @FXML
    public void onClearSelectedBuild() {
        if (selectedChampion == null) return;
        currentBuild().clear();
        refreshBuildSlots();
        refreshItemGrid();
        updateSelectedStats();
        statusLabel.setText("Build cleared for " + selectedChampion.getName() + ".");
    }

    @FXML
    public void onCompare() {
        if (redTeam.isEmpty() || blueTeam.isEmpty()) {
            statusLabel.setText("Add at least one champion to both teams before analyzing.");
            return;
        }

        double redHp = teamStat(redTeam, Stat.HP);
        double blueHp = teamStat(blueTeam, Stat.HP);
        double redAd = teamStat(redTeam, Stat.AD);
        double blueAd = teamStat(blueTeam, Stat.AD);
        double redAp = teamStat(redTeam, Stat.AP);
        double blueAp = teamStat(blueTeam, Stat.AP);

        statusLabel.setText(String.format(
                "Team overview • Red: %.0f HP / %.0f AD / %.0f AP • Blue: %.0f HP / %.0f AD / %.0f AP",
                redHp, redAd, redAp, blueHp, blueAd, blueAp));
    }

    private double teamStat(List<Champion> team, Stat stat) {
        double total = 0;
        for (Champion champion : team) {
            int level = levelsByChampionId.getOrDefault(champion.getId(), 1);
            Build build = new Build();
            List<Item> items = buildsByChampionId.getOrDefault(champion.getId(), List.of());
            items.forEach(build::addItem);
            Stats stats = statCalculator.calculateFinalStats(champion, level, build);
            total += switch (stat) {
                case HP -> stats.getHp();
                case AD -> stats.getAttackDamage();
                case AP -> stats.getAbilityPower();
            };
        }
        return total;
    }

    private String describe(Stats stats) {
        return String.format(
                "HP  %,.0f%nAD  %,.0f%nAP  %,.0f%nArmor  %,.0f%nMR  %,.0f%nAS  %.3f%nMove Speed  %,.0f",
                stats.getHp(), stats.getAttackDamage(), stats.getAbilityPower(), stats.getArmor(),
                stats.getMagicResist(), stats.getAttackSpeed(), stats.getMoveSpeed());
    }

    private List<Item> currentBuild() {
        if (selectedChampion == null) return new ArrayList<>();
        return buildsByChampionId.computeIfAbsent(selectedChampion.getId(), id -> new ArrayList<>());
    }

    private int findItemIndex(List<Item> items, Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == item.getId()) return i;
        }
        return -1;
    }

    private boolean containsChampion(List<Champion> team, Champion champion) {
        return team.stream().anyMatch(c -> c.getId() == champion.getId());
    }

    private String teamOf(Champion champion) {
        if (containsChampion(redTeam, champion)) return "RED";
        if (containsChampion(blueTeam, champion)) return "BLUE";
        return activeTeam;
    }

    private ImageView imageView(String url, double width, double height) {
        ImageView view = new ImageView();
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        Image image = loadImage(url);
        if (image != null) view.setImage(image);
        return view;
    }

    private Image loadImage(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return new Image(url, true);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private enum Stat { HP, AD, AP }

    public void setChampionRepository(ChampionRepository championRepository) { this.championRepository = championRepository; }
    public void setItemRepository(ItemRepository itemRepository) { this.itemRepository = itemRepository; }
    public void setStatCalculator(StatCalculator statCalculator) { this.statCalculator = statCalculator; }
    public void setViewManager(ViewManager viewManager) { this.viewManager = viewManager; }

    @FXML
    public void onSwitchToPrediction() {
        if (viewManager != null) viewManager.showPredictionView();
    }

    @FXML
    public void onSwitchToAdvisor() {
        if (viewManager != null) viewManager.showAdvisorView();
    }
}
