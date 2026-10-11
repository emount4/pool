package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.chart.*;
import ru.mirea.pool.application.service.DataFilters;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.presentation.fx.util.*;
import java.time.LocalDate;
import java.util.*;

public final class StatisticsController {
    @FXML private TilePane cards;
    @FXML private DatePicker fromPicker, toPicker;
    @FXML private Label periodLabel, errorLabel;
    @FXML private BarChart<String,Number> laneChart;
    private MainController main;
    private LocalDate from, to;
    private final List<List<String>> metrics = new ArrayList<>();
    @FXML private void initialize() {
        Ui.datePicker(fromPicker); Ui.datePicker(toPicker);
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());
        cards.prefTileWidthProperty().bind(cards.widthProperty().subtract(28).divide(3));
    }
    public void configure(MainController main) { this.main = main; }
    public void refresh() {
        if (main == null) return;
        var visits = main.visits.stream().filter(v -> DataFilters.inPeriod(v.getVisitDate(), from, to)).toList();
        var stats = main.context.statistics.calculate(main.clients.size(), visits);
        metrics.clear(); cards.getChildren().clear();
        metric("Клиентов в базе", "" + stats.totalClients());
        metric("Посещений за период", "" + stats.totalVisits());
        metric("Посещений сегодня", "" + stats.visitsToday());
        for (var s : VisitStatus.values()) metric(Labels.status(s), "" + stats.visitsByStatus().get(s));
        metric("Средняя длительность", String.format(Locale.forLanguageTag("ru"), "%.1f мин", stats.averageDurationMinutes()));
        metric("Популярная дорожка", stats.mostPopularLane() == null ? "—" : "№ " + stats.mostPopularLane());
        var series = new XYChart.Series<String,Number>();
        for (int lane = 1; lane <= 8; lane++) {
            final int n = lane;
            series.getData().add(new XYChart.Data<>("" + lane, visits.stream().filter(v -> v.getLaneNumber() == n && v.getStatus() != VisitStatus.CANCELLED).count()));
        }
        laneChart.getData().setAll(List.of(series));
        periodLabel.setText("Период: " + (from == null ? "с начала учёта" : from.format(Labels.DATE)) + " — "
                + (to == null ? "без ограничения" : to.format(Labels.DATE)));
    }
    private void metric(String title, String value) {
        metrics.add(List.of(title, value));
        var label = new Label(title); label.getStyleClass().add("metric-title");
        var number = new Label(value); number.getStyleClass().add("metric-value");
        var card = new VBox(10, label, number); card.getStyleClass().add("metric-card"); card.setPrefWidth(286);
        cards.getChildren().add(card);
    }
    @FXML private void applyPeriod() {
        try {
            var a = Ui.readDate(fromPicker, "Начало периода"); var b = Ui.readDate(toPicker, "Конец периода");
            DataFilters.validatePeriod(a,b); from = a; to = b; errorLabel.setText(""); refresh();
        } catch (RuntimeException e) { errorLabel.setText(Ui.message(e)); }
    }
    @FXML private void reset() { from = null; to = null; fromPicker.setValue(null); toPicker.setValue(null); errorLabel.setText(""); refresh(); }
    @FXML private void reload() { main.reload(); }
    @FXML private void export() {
        var rows = new ArrayList<>(List.of(List.of("Период", periodLabel.getText()))); rows.addAll(metrics);
        main.export("statistics", List.of("Показатель", "Значение"), rows);
    }
}
