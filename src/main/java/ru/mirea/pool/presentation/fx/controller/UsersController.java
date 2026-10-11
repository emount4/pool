package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.transformation.*;
import ru.mirea.pool.application.service.DataFilters;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.presentation.fx.util.*;
import java.util.List;

public final class UsersController {
    @FXML private TableView<SystemUser> table;
    @FXML private TextField searchField;
    @FXML private Button editButton;
    private MainController main;
    private FilteredList<SystemUser> filtered;
    @FXML private void initialize() {
        Ui.column(table, "Логин", 250, SystemUser::getUsername);
        Ui.column(table, "Роль", 240, u -> Labels.role(u.getRole()));
        Ui.column(table, "Доступ", 180, u -> u.isActive() ? "Активен" : "Заблокирован");
        Ui.dateColumn(table, "Дата создания", u -> u.getCreatedAt() == null ? null : u.getCreatedAt().toLocalDate());
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        searchField.textProperty().addListener((o,a,b) -> { if (filtered != null) filtered.setPredicate(u -> DataFilters.search(b, u.getUsername(), Labels.role(u.getRole()))); });
    }
    public void configure(MainController main) {
        this.main = main; filtered = new FilteredList<>(main.users);
        var sorted = new SortedList<>(filtered); sorted.comparatorProperty().bind(table.comparatorProperty()); table.setItems(sorted);
    }
    @FXML private void add() { form(null); }
    @FXML private void edit() { var user = table.getSelectionModel().getSelectedItem(); if (user != null) form(user); }
    private void form(SystemUser user) {
        var view = Views.<UserFormController>load("user-form"); view.controller().configure(user);
        view.controller().open(main, user == null ? "Новый пользователь" : "Редактирование пользователя");
    }
    @FXML private void refresh() { main.reload(); }
    @FXML private void export() {
        main.export("users", List.of("Логин", "Роль", "Доступ"), table.getItems().stream()
                .map(u -> List.of(u.getUsername(), Labels.role(u.getRole()), u.isActive() ? "Активен" : "Заблокирован")).toList());
    }
}
