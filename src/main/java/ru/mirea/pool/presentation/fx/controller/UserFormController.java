package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.presentation.fx.util.*;
import java.util.Arrays;

public final class UserFormController extends FormController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField, confirmField;
    @FXML private ComboBox<UserRole> roleBox;
    @FXML private CheckBox activeBox;
    @FXML private Label passwordHint;
    private SystemUser user;
    @FXML private void initialize() {
        roleBox.getItems().setAll(UserRole.values()); roleBox.setConverter(Ui.converter(r -> r == null ? "" : Labels.role(r)));
        roleBox.setValue(UserRole.OPERATOR); activeBox.setSelected(true);
    }
    public void configure(SystemUser user) {
        this.user = user;
        if (user == null) { activeBox.setVisible(false); activeBox.setManaged(false); }
        else {
            usernameField.setText(user.getUsername()); roleBox.setValue(user.getRole()); activeBox.setSelected(user.isActive());
            passwordHint.setText("Оставьте пароль пустым, чтобы сохранить текущий.");
        }
    }
    @FXML private void submit() {
        validation(() -> {
            if (!passwordField.getText().equals(confirmField.getText())) throw new ValidationException("Пароли не совпадают.");
            var username = usernameField.getText(); var role = roleBox.getValue(); var active = activeBox.isSelected();
            char[] password = passwordField.getText().toCharArray();
            save(() -> {
                try {
                    if (user == null) main.context.users.createUser(main.session, username, password, role);
                    else main.context.users.updateUser(main.session, user.getId(), username, role, active, password);
                    return true;
                } finally { Arrays.fill(password, '\0'); }
            });
        });
    }
}
