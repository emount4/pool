package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import ru.mirea.pool.application.AppContext;
import ru.mirea.pool.presentation.fx.PoolApplication;
import ru.mirea.pool.presentation.fx.util.*;
import java.util.Arrays;

public final class LoginController {
    @FXML private VBox root;
    @FXML private Label statusLabel;
    @FXML private TextField usernameField;
    @FXML private TextField visiblePasswordField, visibleConfirmField;
    @FXML private PasswordField passwordField, confirmField;
    @FXML private ToggleButton passwordToggle;
    @FXML private VBox confirmation;
    @FXML private Button loginButton;
    private PoolApplication application;
    private FxTasks tasks;
    private AppContext context;
    private boolean bootstrap;

    @FXML private void initialize() {
        Ui.passwordVisibility(passwordField, visiblePasswordField, passwordToggle);
        Ui.passwordVisibility(confirmField, visibleConfirmField, passwordToggle);
        passwordToggle.textProperty().bind(javafx.beans.binding.Bindings.when(passwordToggle.selectedProperty())
                .then("Скрыть").otherwise("Показать"));
    }

    public void configure(PoolApplication application, FxTasks tasks) {
        this.application = application; this.tasks = tasks;
        checkConnection();
    }
    @FXML private void checkConnection() {
        tasks.run(root, statusLabel, "Подключение к базе…", () -> {
            var ctx = new AppContext();
            return new Ready(ctx, ctx.bootstrap.isBootstrapRequired());
        }, ready -> {
            context = ready.context(); bootstrap = ready.bootstrap();
            confirmation.setVisible(bootstrap); confirmation.setManaged(bootstrap);
            loginButton.setDisable(false);
            loginButton.setText(bootstrap ? "Создать администратора" : "Войти");
            statusLabel.setText(bootstrap ? "База пуста. Создайте первого администратора." : "Введите логин и пароль для входа.");
        }, error -> {
            context = null; loginButton.setDisable(true);
            statusLabel.setText(Ui.message(error));
        });
    }
    @FXML private void login() {
        String username = usernameField.getText();
        if (username.isBlank() || passwordField.getText().isEmpty()) {
            statusLabel.setText("Введите логин и пароль."); return;
        }
        if (bootstrap && !passwordField.getText().equals(confirmField.getText())) {
            statusLabel.setText("Пароли не совпадают."); return;
        }
        char[] password = passwordField.getText().toCharArray();
        tasks.run(root, statusLabel, "Вход…", () -> {
            try {
                if (bootstrap) context.bootstrap.createFirstAdmin(username, password);
                return context.auth.login(username, password);
            } finally { Arrays.fill(password, '\0'); }
        }, session -> {
            passwordField.clear(); confirmField.clear();
            passwordToggle.setSelected(false);
            application.showMain(context, session);
        }, error -> {
            passwordField.clear(); confirmField.clear();
            passwordToggle.setSelected(false);
            statusLabel.setText(Ui.message(error));
        });
    }
    private record Ready(AppContext context, boolean bootstrap) { }
}
