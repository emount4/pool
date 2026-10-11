package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.presentation.fx.util.Ui;

public final class ClientFormController extends FormController {
    @FXML private TextField firstNameField, lastNameField, phoneField, emailField;
    @FXML private DatePicker birthDatePicker;
    private Client client;
    @FXML private void initialize() { Ui.datePicker(birthDatePicker); }
    public void configure(Client client) {
        this.client = client;
        if (client != null) {
            firstNameField.setText(client.getFirstName()); lastNameField.setText(client.getLastName());
            phoneField.setText(client.getPhone()); emailField.setText(client.getEmail());
            birthDatePicker.setValue(client.getBirthDate());
        }
    }
    @FXML private void submit() {
        validation(() -> {
            var birth = Ui.readDate(birthDatePicker, "Дата рождения");
            String first = firstNameField.getText(), last = lastNameField.getText(), phone = phoneField.getText(), email = emailField.getText();
            save(() -> client == null ? main.context.clients.createClient(first, last, phone, email, birth)
                    : main.context.clients.updateClient(client.getId(), first, last, phone, email, birth));
        });
    }
}
