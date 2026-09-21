package com.marclw.lolstats.ui.prediction;

import com.marclw.lolstats.prediction.PredictionService;
import com.marclw.lolstats.prediction.WinProbabilityResult;
import com.marclw.lolstats.ui.ViewManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

/**
 * Wires up the match-prediction screen: match-ID input, a minute selector,
 * a "Predict" action that calls PredictionService, and a win-probability
 * display (bar + a probability-over-time chart).
 *
 * Prediction hits the Riot API, so it runs on a background Task rather than
 * the JavaFX Application Thread - otherwise the whole window freezes for the
 * duration of two HTTP round trips. This is the same problem flagged for
 * MainController.populateData(); fixed here from the start.
 */
public class PredictionController {

    private PredictionService predictionService;

    // Set by ViewManager right after this controller is loaded, so this
    // screen can ask to switch back to the calculator view.
    private ViewManager viewManager;

    @FXML private TextField matchIdField;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Button predictButton;
    @FXML private Label statusLabel;
    @FXML private Label blueProbabilityLabel;
    @FXML private Label redProbabilityLabel;
    @FXML private ProgressBar blueProbabilityBar;
    @FXML private LineChart<Number, Number> probabilityChart;
    @FXML private Label predictionBasisLabel;

    public void initialize() {
        minuteSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 15));
        statusLabel.setText("Enter a match ID and press Predict.");
        blueProbabilityBar.setProgress(0);
    }

    /**
     * Called by the Predict button. Fetches + scores off-thread, then
     * publishes results back onto the FX thread.
     */
    @FXML
    public void onPredictButtonClicked() {
        String matchId = matchIdField.getText() == null ? "" : matchIdField.getText().trim();
        if (matchId.isEmpty()) {
            statusLabel.setText("Enter a match ID first (e.g. EUW1_7975080416).");
            return;
        }
        if (predictionService == null) {
            statusLabel.setText("Prediction service unavailable - check API key and model setup.");
            return;
        }

        int throughMinute = minuteSpinner.getValue();
        setBusy(true);
        statusLabel.setText("Fetching match and scoring...");

        Task<WinProbabilityResult[]> task = new Task<>() {
            @Override
            protected WinProbabilityResult[] call() {
                return predictionService.predictOverTime(matchId, throughMinute);
            }
        };

        task.setOnSucceeded(event -> {
            WinProbabilityResult[] results = task.getValue();
            displayResults(results);
            setBusy(false);
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            // Surface the real reason rather than a generic failure: the two
            // most common causes here are an expired dev key (24h) and no
            // trained model on disk yet, and the user can act on either.
            statusLabel.setText("Prediction failed: "
                    + (error == null ? "unknown error" : error.getMessage()));
            setBusy(false);
        });

        Thread thread = new Thread(task, "prediction-fetch");
        thread.setDaemon(true); // don't keep the JVM alive after the window closes
        thread.start();
    }

    private void displayResults(WinProbabilityResult[] results) {
        if (results == null || results.length == 0) {
            statusLabel.setText("No timeline data available for that match.");
            return;
        }

        WinProbabilityResult finalResult = results[results.length - 1];
        double blue = finalResult.getBlueWinProbability();

        blueProbabilityBar.setProgress(blue);
        blueProbabilityLabel.setText(String.format("Blue: %.1f%%", blue * 100));
        redProbabilityLabel.setText(String.format("Red:  %.1f%%", finalResult.getRedWinProbability() * 100));
        statusLabel.setText(String.format(
                "Win probability as of minute %d of match %s.",
                finalResult.getMinute(), finalResult.getMatchId()));

        predictionBasisLabel.setText(describeBasis(finalResult));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Blue win probability");
        for (WinProbabilityResult result : results) {
            series.getData().add(new XYChart.Data<>(result.getMinute(), result.getBlueWinProbability()));
        }
        probabilityChart.getData().clear();
        probabilityChart.getData().add(series);
    }

    private String describeBasis(WinProbabilityResult result) {
        java.util.Map<String, Double> features = result.getFeatures();
        if (features == null) {
            return "Feature breakdown unavailable for this prediction.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(diffLine("Gold", features.get("goldDiffAtMinute"), false));
        sb.append(diffLine("XP", features.get("xpDiffAtMinute"), false));
        sb.append(diffLine("Kills", features.get("killDiffAtMinute"), true));
        sb.append(diffLine("Dragons", features.get("dragonDiffAtMinute"), true));
        sb.append(diffLine("Heralds", features.get("heraldDiffAtMinute"), true));
        sb.append(diffLine("Towers", features.get("towerDiffAtMinute"), true));
        return sb.toString().stripTrailing();
    }

    private String diffLine(String label, Double diff, boolean wholeNumber) {
        if (diff == null) {
            return String.format("%-9s n/a%n", label + ":");
        }
        String magnitude = wholeNumber
                ? String.valueOf(Math.abs(diff.intValue()))
                : String.format("%,.0f", Math.abs(diff));
        String who = diff > 0 ? "Blue ahead" : diff < 0 ? "Red ahead" : "Even";
        return String.format("%-9s %s (%s)%n", label + ":", magnitude, who);
    }

    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            predictButton.setDisable(busy);
            matchIdField.setDisable(busy);
            minuteSpinner.setDisable(busy);
        });
    }

    /**
     * Called by the "Back to Stat Calculator" button (see PredictionView.fxml,
     * onAction="#onSwitchToCalculator").
     */
    @FXML
    public void onSwitchToCalculator() {
        if (viewManager != null) {
            viewManager.showCalculatorView();
        }
    }

    @FXML
    public void onSwitchToAdvisor() {
        if (viewManager != null) {
            viewManager.showAdvisorView();
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