package ru.mirea.pool.presentation.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mirea.pool.application.AppContext;
import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.presentation.fx.controller.LoginController;
import ru.mirea.pool.presentation.fx.controller.MainController;
import ru.mirea.pool.presentation.fx.util.*;

public final class PoolApplication extends Application {
    private Stage stage;
    private final FxTasks tasks = new FxTasks();

    @Override public void start(Stage stage) {
        this.stage = stage;
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) ->
                Platform.runLater(() -> Ui.error(stage, error)));
        stage.setTitle("Бассейн • управление посещениями");
        showLogin();
        stage.show();
    }

    public void showLogin() {
        var view = Views.<LoginController>load("login");
        view.controller().configure(this, tasks);
        stage.setMinWidth(700);
        stage.setMinHeight(620);
        stage.setScene(Views.scene(view.root(), 820, 660));
        stage.centerOnScreen();
    }

    public void showMain(AppContext context, UserSession session) {
        var view = Views.<MainController>load("main");
        view.controller().configure(context, session, tasks, this);
        stage.setMinWidth(1020);
        stage.setMinHeight(740);
        Scene scene = Views.scene(view.root(), 1320, 860);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    @Override public void stop() { tasks.close(); }
    public static void main(String[] args) { launch(args); }
}
