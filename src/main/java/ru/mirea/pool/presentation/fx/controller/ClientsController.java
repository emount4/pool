package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.transformation.*;
import javafx.beans.binding.Bindings;
import ru.mirea.pool.application.service.DataFilters;
import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.presentation.fx.util.*;
import java.time.LocalDate;
import java.util.List;

public final class ClientsController {
    @FXML private TableView<Client> table;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> ageFilter, emailFilter;
    @FXML private Button editButton, deleteButton, historyButton;
    @FXML private Label countLabel;
    private MainController main;
    private FilteredList<Client> filtered;
    @FXML private void initialize() {
        Ui.column(table, "Фамилия", 150, Client::getLastName);
        Ui.column(table, "Имя", 140, Client::getFirstName);
        Ui.column(table, "Телефон", 160, Client::getPhone);
        Ui.column(table, "Email", 240, Client::getEmail);
        Ui.dateColumn(table, "Дата рождения", Client::getBirthDate);
        Ui.column(table, "Возраст", 85, c -> java.time.Period.between(c.getBirthDate(), LocalDate.now()).getYears());
        ageFilter.getItems().setAll("Любой возраст", "До 18 лет", "18 лет и старше");
        emailFilter.getItems().setAll("Любой email", "Email указан", "Без email");
        ageFilter.getSelectionModel().selectFirst(); emailFilter.getSelectionModel().selectFirst();
        for (var button : List.of(editButton, deleteButton, historyButton))
            button.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        searchField.textProperty().addListener((o,a,b) -> applyFilters());
        ageFilter.valueProperty().addListener((o,a,b) -> applyFilters());
        emailFilter.valueProperty().addListener((o,a,b) -> applyFilters());
        table.setRowFactory(t -> { var row = new TableRow<Client>(); row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) edit(); }); return row; });
    }
    public void configure(MainController main) {
        this.main = main;
        filtered = new FilteredList<>(main.clients);
        var sorted = new SortedList<>(filtered); sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        countLabel.textProperty().bind(Bindings.size(filtered).asString("Показано клиентов: %d"));
        table.getSortOrder().setAll(List.of(table.getColumns().getFirst()));
    }
    @FXML private void applyFilters() {
        if (filtered == null) return;
        filtered.setPredicate(c -> DataFilters.client(c, searchField.getText(), ageFilter.getSelectionModel().getSelectedIndex(),
                emailFilter.getSelectionModel().getSelectedIndex(), LocalDate.now()));
    }
    @FXML private void reset() { searchField.clear(); ageFilter.getSelectionModel().selectFirst(); emailFilter.getSelectionModel().selectFirst(); }
    @FXML private void add() { form(null); }
    @FXML private void edit() { if (table.getSelectionModel().getSelectedItem() != null) form(table.getSelectionModel().getSelectedItem()); }
    private void form(Client client) {
        var view = Views.<ClientFormController>load("client-form"); view.controller().configure(client);
        view.controller().open(main, client == null ? "Новый клиент" : "Редактирование клиента");
    }
    @FXML private void delete() {
        var client = table.getSelectionModel().getSelectedItem();
        if (client != null && Ui.confirm(main.owner(), "Удалить клиента?", Labels.client(client) + "\nКлиента с посещениями удалить нельзя."))
            main.mutate("Удаление клиента…", () -> main.context.clients.deleteClient(client.getId()));
    }
    @FXML private void history() { var c = table.getSelectionModel().getSelectedItem(); if (c != null) main.showVisits(c); }
    @FXML private void refresh() { main.reload(); }
    @FXML private void export() {
        main.export("clients", List.of("Фамилия", "Имя", "Телефон", "Email", "Дата рождения"), table.getItems().stream()
                .map(c -> List.of(c.getLastName(), c.getFirstName(), c.getPhone(), c.getEmail() == null ? "" : c.getEmail(), c.getBirthDate().format(Labels.DATE))).toList());
    }
}
