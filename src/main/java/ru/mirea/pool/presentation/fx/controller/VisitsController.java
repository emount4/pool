package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.*;
import ru.mirea.pool.application.service.DataFilters;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.presentation.fx.util.*;
import java.time.*;
import java.util.List;

public final class VisitsController {
    @FXML private TableView<Visit> table;
    @FXML private TextField searchField;
    @FXML private ComboBox<VisitStatus> statusFilter;
    @FXML private ComboBox<Integer> laneFilter;
    @FXML private DatePicker fromPicker, toPicker;
    @FXML private Label countLabel, filterError, clientLabel, detailsLabel;
    @FXML private Button editButton, cancelButton, deleteButton;
    private MainController main;
    private FilteredList<Visit> filtered;
    private Long clientId;
    private LocalDate from, to;
    @FXML private void initialize() {
        Ui.column(table, "Клиент", 220, v -> Labels.client(main.client(v.getClientId())));
        Ui.column(table, "Телефон", 160, v -> main.client(v.getClientId()) == null ? "" : main.client(v.getClientId()).getPhone());
        Ui.dateColumn(table, "Дата", Visit::getVisitDate);
        Ui.column(table, "Начало", 110, Visit::getStartTime);
        Ui.column(table, "Окончание", 130, Visit::getEndTime);
        Ui.column(table, "Минут", 75, Visit::getDurationMinutes);
        Ui.column(table, "Дорожка", 95, Visit::getLaneNumber);
        var statusColumn = Ui.column(table, "Статус", 155, Visit::getStatus);
        statusColumn.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(VisitStatus status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-planned", "status-completed", "status-in_progress", "status-cancelled");
                setText(empty || status == null ? null : Labels.status(status));
                if (!empty && status != null) getStyleClass().add("status-" + status.name().toLowerCase(java.util.Locale.ROOT));
            }
        });
        statusFilter.setConverter(Ui.converter(Labels::status));
        statusFilter.getItems().add(null); statusFilter.getItems().addAll(VisitStatus.values());
        laneFilter.setConverter(Ui.converter(n -> n == null ? "Все дорожки" : "Дорожка " + n));
        laneFilter.getItems().add(null); for (int i = 1; i <= 8; i++) laneFilter.getItems().add(i);
        statusFilter.getSelectionModel().selectFirst(); laneFilter.getSelectionModel().selectFirst();
        filterError.managedProperty().bind(filterError.textProperty().isNotEmpty());
        filterError.visibleProperty().bind(filterError.managedProperty());
        clientLabel.managedProperty().bind(clientLabel.textProperty().isNotEmpty());
        clientLabel.visibleProperty().bind(clientLabel.managedProperty());
        Ui.datePicker(fromPicker); Ui.datePicker(toPicker);
        searchField.textProperty().addListener((o,a,b) -> applyFilters());
        statusFilter.valueProperty().addListener((o,a,b) -> applyFilters());
        laneFilter.valueProperty().addListener((o,a,b) -> applyFilters());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        var cannotEdit = Bindings.createBooleanBinding(() -> !editable(table.getSelectionModel().getSelectedItem()), table.getSelectionModel().selectedItemProperty());
        editButton.disableProperty().bind(cannotEdit); cancelButton.disableProperty().bind(cannotEdit);
        table.getSelectionModel().selectedItemProperty().addListener((o,a,v) -> detailsLabel.setText(v == null ? "Выберите посещение, чтобы увидеть данные клиента."
                : clientDetails(v)));
        table.setRowFactory(t -> { var row = new TableRow<Visit>(); row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty() && editable(row.getItem())) edit(); }); return row; });
    }
    private String clientDetails(Visit v) {
        Client c = main.client(v.getClientId());
        return c == null ? "Клиент не найден. Обновите данные." : Labels.client(c) + "  ·  " + c.getPhone()
                + "  ·  " + (c.getEmail() == null ? "Email не указан" : c.getEmail()) + "  ·  Дата рождения: " + c.getBirthDate().format(Labels.DATE);
    }
    private boolean editable(Visit v) {
        return v != null && v.getStatus() == VisitStatus.PLANNED && LocalDateTime.of(v.getVisitDate(), v.getStartTime()).isAfter(LocalDateTime.now());
    }
    public void configure(MainController main) {
        this.main = main; filtered = new FilteredList<>(main.visits);
        var sorted = new SortedList<>(filtered); sorted.comparatorProperty().bind(table.comparatorProperty()); table.setItems(sorted);
        countLabel.textProperty().bind(Bindings.size(filtered).asString("Показано посещений: %d"));
        table.getSortOrder().setAll(List.of(table.getColumns().get(2), table.getColumns().get(3)));
    }
    @FXML public void applyFilters() {
        if (filtered == null) return;
        filtered.setPredicate(v -> DataFilters.visit(v, main.client(v.getClientId()), searchField.getText(), statusFilter.getValue(),
                laneFilter.getValue(), from, to, clientId));
    }
    @FXML private void applyPeriod() {
        try {
            var newFrom = Ui.readDate(fromPicker, "Начало периода"); var newTo = Ui.readDate(toPicker, "Конец периода");
            DataFilters.validatePeriod(newFrom, newTo); from = newFrom; to = newTo; filterError.setText(""); applyFilters();
        } catch (RuntimeException e) { filterError.setText(Ui.message(e) + " Текущий фильтр сохранён."); }
    }
    public void forClient(Client client) {
        reset(); clientId = client.getId(); clientLabel.setText("Посещения клиента: " + Labels.client(client)); applyFilters();
    }
    @FXML private void reset() {
        clientId = null; from = null; to = null; fromPicker.setValue(null); toPicker.setValue(null);
        searchField.clear(); statusFilter.getSelectionModel().selectFirst(); laneFilter.getSelectionModel().selectFirst();
        clientLabel.setText(""); filterError.setText(""); applyFilters();
    }
    @FXML private void add() {
        if (main.clients.isEmpty()) { Ui.info(main.owner(), "Нет клиентов", "Сначала добавьте клиента на вкладке «Клиенты»."); return; }
        form(null);
    }
    @FXML private void edit() { var v = table.getSelectionModel().getSelectedItem(); if (editable(v)) form(v); }
    private void form(Visit visit) {
        var view = Views.<VisitFormController>load("visit-form"); view.controller().configure(main, visit);
        view.controller().open(main, visit == null ? "Новое посещение" : "Редактирование посещения");
    }
    @FXML private void cancelVisit() {
        var v = table.getSelectionModel().getSelectedItem();
        if (v != null && Ui.confirm(main.owner(), "Отменить посещение?", describe(v) + "\nЗапись останется в истории. Время и дорожка освободятся."))
            main.mutate("Отмена посещения…", () -> main.context.visits.cancelVisit(v.getId()));
    }
    @FXML private void delete() {
        var v = table.getSelectionModel().getSelectedItem();
        if (v != null && Ui.confirm(main.owner(), "Удалить посещение?", describe(v) + "\nЗапись будет удалена из базы без возможности восстановления."))
            main.mutate("Удаление посещения…", () -> main.context.visits.deleteVisit(v.getId()));
    }
    private String describe(Visit v) { return Labels.client(main.client(v.getClientId())) + " · " + v.getVisitDate().format(Labels.DATE) + " в " + v.getStartTime().format(Labels.TIME); }
    @FXML private void refresh() { main.reload(); }
    @FXML private void export() {
        main.export("visits", List.of("Клиент", "Телефон", "Дата", "Начало", "Окончание", "Минут", "Дорожка", "Статус"),
                table.getItems().stream().map(v -> List.of(Labels.client(main.client(v.getClientId())), main.client(v.getClientId()) == null ? "" : main.client(v.getClientId()).getPhone(),
                        v.getVisitDate().format(Labels.DATE), v.getStartTime().format(Labels.TIME), v.getEndTime().format(Labels.TIME),
                        "" + v.getDurationMinutes(), "" + v.getLaneNumber(), Labels.status(v.getStatus()))).toList());
    }
}
