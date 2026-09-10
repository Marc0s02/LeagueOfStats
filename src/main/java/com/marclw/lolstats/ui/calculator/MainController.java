package com.marclw.lolstats.ui.calculator;

import com.marclw.lolstats.ingest.ChampionRepository;
import com.marclw.lolstats.ingest.ItemRepository;
import com.marclw.lolstats.model.Build;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.Item;
import com.marclw.lolstats.model.Stats;
import com.marclw.lolstats.service.StatCalculator;
import com.marclw.lolstats.ui.ViewManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;

/**
 * Wires up a side-by-side champion comparison: pick a champion + level +
 * item build on each side, hit Compare, see final stats for both.
 */
public class MainController {

    private ChampionRepository championRepository;
    private ItemRepository itemRepository;
    private StatCalculator statCalculator;
    private ViewManager viewManager;

    @FXML private ComboBox<Champion> championABox;
    @FXML private ComboBox<Champion> championBBox;
    @FXML private Spinner<Integer> levelASpinner;
    @FXML private Spinner<Integer> levelBSpinner;
    @FXML private ListView<Item> itemsAListView;
    @FXML private ListView<Item> itemsBListView;
    @FXML private Label statsALabel;
    @FXML private Label statsBLabel;

    /**
     * Runs automatically when FXMLLoader loads the view - BEFORE
     * ViewManager has a chance to inject championRepository/itemRepository/
     * statCalculator (those come after loader.load() returns). So this only
     * sets up things that don't depend on data: spinner ranges, multi-select
     * mode, and cell rendering. Actually populating the combo/list boxes
     * happens in populateData(), called by ViewManager once injection is done.
     */
    public void initialize() {
        levelASpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 18, 1));
        levelBSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 18, 1));

        itemsAListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsBListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        StringConverter<Champion> championDisplay = new StringConverter<>() {
            @Override
            public String toString(Champion champion) {
                return champion == null ? "" : champion.getName();
            }

            @Override
            public Champion fromString(String string) {
                return null; // combo boxes here aren't user-editable text fields
            }
        };
        championABox.setConverter(championDisplay);
        championBBox.setConverter(championDisplay);

        itemsAListView.setCellFactory(list -> itemCell());
        itemsBListView.setCellFactory(list -> itemCell());
    }

    private ListCell<Item> itemCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
    }

    /**
     * Called by ViewManager right after championRepository/itemRepository/
     * statCalculator are set - actually loads and populates the combo/list
     * boxes, which can't happen inside initialize() since the repositories
     * don't exist yet at that point.
     */
    public void populateData() {
        List<Champion> champions = championRepository.getChampions().isEmpty()
                ? championRepository.loadAll() : championRepository.getChampions();
        List<Item> items = itemRepository.getItems().isEmpty()
                ? itemRepository.loadAll() : itemRepository.getItems();

        ObservableList<Champion> championOptions = FXCollections.observableArrayList(champions);
        championOptions.sort(Comparator.comparing(Champion::getName));
        championABox.setItems(championOptions);
        championBBox.setItems(championOptions);

        ObservableList<Item> itemOptions = FXCollections.observableArrayList(items);
        itemOptions.sort(Comparator.comparing(Item::getName));
        itemsAListView.setItems(itemOptions);
        itemsBListView.setItems(itemOptions);

        if (!championOptions.isEmpty()) {
            championABox.getSelectionModel().selectFirst();
            championBBox.getSelectionModel().select(Math.min(1, championOptions.size() - 1));
        }
    }

    @FXML
    public void onCompare() {
        Champion championA = championABox.getValue();
        Champion championB = championBBox.getValue();

        if (championA == null || championB == null) {
            statsALabel.setText("Select a champion on both sides first.");
            statsBLabel.setText("");
            return;
        }

        Build buildA = new Build();
        for (Item item : itemsAListView.getSelectionModel().getSelectedItems()) {
            buildA.addItem(item);
        }
        Build buildB = new Build();
        for (Item item : itemsBListView.getSelectionModel().getSelectedItems()) {
            buildB.addItem(item);
        }

        Stats statsA = statCalculator.calculateFinalStats(championA, levelASpinner.getValue(), buildA);
        Stats statsB = statCalculator.calculateFinalStats(championB, levelBSpinner.getValue(), buildB);

        statsALabel.setText(describe(statsA));
        statsBLabel.setText(describe(statsB));
    }

    private String describe(Stats stats) {
        return String.format(
                "HP: %.0f%nAttack Damage: %.0f%nAbility Power: %.0f%nArmor: %.0f%n"
                        + "Magic Resist: %.0f%nAttack Speed: %.3f%nMove Speed: %.0f",
                stats.getHp(), stats.getAttackDamage(), stats.getAbilityPower(),
                stats.getArmor(), stats.getMagicResist(), stats.getAttackSpeed(), stats.getMoveSpeed());
    }

    /**
     * Called by the "Switch to Match Prediction" button.
     */
    @FXML
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

    public ItemRepository getItemRepository() {
        return itemRepository;
    }

    public void setItemRepository(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
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