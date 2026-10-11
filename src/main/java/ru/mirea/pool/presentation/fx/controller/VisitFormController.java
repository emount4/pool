package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.binding.Bindings;
import ru.mirea.pool.application.dto.VisitSlotDto;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.presentation.fx.util.*;
import java.time.*;
import java.util.List;

public final class VisitFormController extends FormController {
    @FXML private ComboBox<Client> clientBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Integer> durationBox, laneBox;
    @FXML private ComboBox<VisitSlotDto> slotBox;
    @FXML private Label slotHint;
    @FXML private Button saveButton;
    private Visit visit;
    @FXML private void initialize() {
        Ui.datePicker(datePicker);
        clientBox.setConverter(Ui.converter(c -> c == null ? "" : Labels.client(c) + " · " + c.getPhone()));
        durationBox.setConverter(Ui.converter(n -> n == null ? "" : n + " минут"));
        durationBox.getItems().setAll(30, 60, 90, 120, 150, 180);
        laneBox.setConverter(Ui.converter(n -> n == null ? "" : "Дорожка " + n));
        slotBox.setConverter(Ui.converter(s -> s == null ? "" : s.startTime().format(Labels.TIME) + " – " + s.endTime().format(Labels.TIME)));
        slotBox.valueProperty().addListener((o, a, b) -> {
            laneBox.getItems().setAll(b == null ? List.of() : b.availableLaneNumbers());
            if (visit != null && laneBox.getItems().contains(visit.getLaneNumber())) laneBox.setValue(visit.getLaneNumber());
            else laneBox.getSelectionModel().selectFirst();
            slotHint.setText(b == null ? "Выберите клиента, дату и длительность." : "Свободных дорожек: " + b.availableLaneNumbers().size());
        });
        saveButton.disableProperty().bind(Bindings.or(slotBox.valueProperty().isNull(), laneBox.valueProperty().isNull()));
    }
    public void configure(MainController main, Visit visit) {
        this.main = main; this.visit = visit;
        clientBox.getItems().setAll(main.clients.stream().sorted(java.util.Comparator.comparing(Labels::client)).toList());
        if (visit == null) {
            clientBox.getSelectionModel().selectFirst(); datePicker.setValue(LocalDate.now().plusDays(1)); durationBox.setValue(60);
        } else {
            clientBox.setValue(main.client(visit.getClientId())); datePicker.setValue(visit.getVisitDate()); durationBox.setValue(visit.getDurationMinutes());
        }
        clientBox.valueProperty().addListener((o,a,b) -> invalidateSlots());
        datePicker.valueProperty().addListener((o,a,b) -> invalidateSlots());
        datePicker.getEditor().textProperty().addListener((o,a,b) -> invalidateSlots());
        durationBox.valueProperty().addListener((o,a,b) -> invalidateSlots());
        refreshSlots();
    }
    private void invalidateSlots() {
        slotBox.getItems().clear(); laneBox.getItems().clear();
        slotHint.setText("Нажмите «Найти свободное время» после изменения условий.");
    }
    @FXML private void refreshSlots() {
        validation(() -> {
            var client = clientBox.getValue(); var date = Ui.readDate(datePicker, "Дата посещения"); var duration = durationBox.getValue();
            if (client == null || date == null || duration == null) throw new ValidationException("Выберите клиента, дату и длительность.");
            invalidateSlots();
            main.tasks.run(root, errorLabel, "Поиск свободного времени…", () -> visit == null
                    ? main.context.visits.getAvailableSlots(client.getId(), date, duration)
                    : main.context.visits.getAvailableSlotsForVisit(visit.getId(), client.getId(), date, duration), slots -> {
                slotBox.getItems().setAll(slots);
                if (visit != null) slots.stream().filter(s -> s.startTime().equals(visit.getStartTime())).findFirst().ifPresent(slotBox::setValue);
                if (slotBox.getValue() == null) slotBox.getSelectionModel().selectFirst();
                errorLabel.setText(slots.isEmpty() ? "Свободного времени нет. Измените дату или длительность." : "Доступных интервалов: " + slots.size());
            }, error -> errorLabel.setText(Ui.message(error)));
        });
    }
    @FXML private void submit() {
        validation(() -> {
            var client = clientBox.getValue(); var date = Ui.readDate(datePicker, "Дата посещения");
            var duration = durationBox.getValue(); var slot = slotBox.getValue(); var lane = laneBox.getValue();
            if (client == null || slot == null || lane == null || duration == null) throw new ValidationException("Найдите свободное время и выберите дорожку.");
            save(() -> visit == null ? main.context.visits.createVisit(main.session, client.getId(), date, slot.startTime(), duration, lane)
                    : main.context.visits.updateVisit(visit.getId(), client.getId(), date, slot.startTime(), duration, lane));
        });
    }
}
