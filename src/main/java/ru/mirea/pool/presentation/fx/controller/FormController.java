package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.*;
import ru.mirea.pool.presentation.fx.util.*;
import java.util.concurrent.Callable;

public abstract class FormController {
    @FXML protected VBox root;
    @FXML protected Label errorLabel;
    protected MainController main;
    private Stage stage;
    public void open(MainController main, String title) {
        this.main = main;
        stage = new Stage();
        stage.initOwner(main.owner()); stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        root.setMinHeight(Region.USE_PREF_SIZE);
        var scroll = new ScrollPane(root);
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        double height = Math.min(root.getPrefHeight() + 4, Screen.getPrimary().getVisualBounds().getHeight() - 70);
        stage.setScene(Views.scene(scroll, 560, height));
        stage.setMinWidth(540); stage.setMinHeight(500); stage.setResizable(true);
        stage.setOnCloseRequest(event -> { if (root.isDisable()) event.consume(); });
        stage.showAndWait();
    }
    protected void save(Callable<?> action) {
        main.tasks.run(root, errorLabel, "Сохранение…", action, ignored -> {
            stage.close(); main.reload();
        }, error -> { errorLabel.setText(Ui.message(error)); });
    }
    protected void validation(Runnable action) {
        try { action.run(); } catch (RuntimeException e) { errorLabel.setText(Ui.message(e)); }
    }
    @FXML protected void cancel() { stage.close(); }
}
