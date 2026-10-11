package ru.mirea.pool.presentation.fx.util;

import javafx.scene.control.*;
import javafx.stage.Window;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.function.Function;
import ru.mirea.pool.application.auth.AuthenticationException;
import ru.mirea.pool.domain.exception.*;
import ru.mirea.pool.infrastructure.exception.DatabaseException;
import ru.mirea.pool.infrastructure.export.ExportException;
import org.slf4j.LoggerFactory;

public final class Ui {
    private Ui() { }
    public static void passwordVisibility(PasswordField masked, TextField visible, ToggleButton toggle) {
        visible.textProperty().bindBidirectional(masked.textProperty());
        visible.visibleProperty().bind(toggle.selectedProperty());
        visible.managedProperty().bind(toggle.selectedProperty());
        masked.visibleProperty().bind(toggle.selectedProperty().not());
        masked.managedProperty().bind(toggle.selectedProperty().not());
    }
    public static String message(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                String state = sql.getSQLState();
                if ("23505".equals(state)) return "Такой телефон, email или логин уже используется. Измените значение.";
                if ("23503".equals(state)) return "У записи есть связанные данные, либо выбранный объект уже удалён. Обновите список.";
                if ("23P01".equals(state)) return "Выбранное время уже занято клиентом или другой записью на дорожку. Обновите свободные интервалы.";
                if ("23514".equals(state) || "22001".equals(state)) return "Данные не соответствуют ограничениям базы. Проверьте длину полей, даты и числа.";
                if ("42P01".equals(state)) return "Таблицы не найдены. Инициализируйте базу скриптами из каталога database.";
                if (state != null && (state.startsWith("08") || state.startsWith("28")))
                    return "Нет подключения к PostgreSQL. Запустите Docker и проверьте адрес, порт и пароль в database.properties.";
            }
        }
        if (error instanceof ValidationException || error instanceof BusinessRuleException ||
                error instanceof EntityNotFoundException || error instanceof AccessDeniedException ||
                error instanceof AuthenticationException || error instanceof ExportException || error instanceof DatabaseException)
            return error.getMessage();
        return "Операция не выполнена. Повторите попытку; подробности сохранены в журнале приложения.";
    }
    public static void error(Window owner, Throwable error) {
        LoggerFactory.getLogger(Ui.class).error("Операция приложения завершилась ошибкой", error);
        alert(owner, Alert.AlertType.ERROR, "Не удалось выполнить действие", message(error));
    }
    public static void info(Window owner, String title, String text) { alert(owner, Alert.AlertType.INFORMATION, title, text); }
    private static void alert(Window owner, Alert.AlertType type, String title, String text) {
        var a = new Alert(type);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(text);
        a.getDialogPane().getStylesheets().add(Views.stylesheet());
        a.showAndWait();
    }
    public static boolean confirm(Window owner, String title, String text) {
        var yes = new ButtonType("Подтвердить", ButtonBar.ButtonData.OK_DONE);
        var a = new Alert(Alert.AlertType.CONFIRMATION, text, yes, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
        a.initOwner(owner); a.setTitle(title); a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(Views.stylesheet());
        return a.showAndWait().orElse(ButtonType.CANCEL) == yes;
    }
    public static <T, V> TableColumn<T, V> column(TableView<T> table, String title, double width, Function<T,V> value) {
        var column = new TableColumn<T,V>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(value.apply(c.getValue())));
        table.getColumns().add(column);
        return column;
    }
    public static <T> void dateColumn(TableView<T> table, String title, Function<T, LocalDate> getter) {
        var column = column(table, title, 130, getter);
        column.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(Labels.DATE));
            }
        });
    }
    public static <T> StringConverter<T> converter(Function<T,String> label) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return label.apply(value); }
            @Override public T fromString(String text) { throw new UnsupportedOperationException("Выберите значение из списка"); }
        };
    }
    public static void datePicker(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : date.format(Labels.DATE); }
            @Override public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                return LocalDate.parse(text.trim(), java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu")
                        .withResolverStyle(java.time.format.ResolverStyle.STRICT));
            }
        });
        picker.setPromptText("дд.мм.гггг");
    }
    public static LocalDate readDate(DatePicker picker, String name) {
        try {
            LocalDate date = picker.getConverter().fromString(picker.getEditor().getText());
            picker.setValue(date);
            return date;
        } catch (RuntimeException e) { throw new ValidationException(name + ": укажите существующую дату в формате дд.мм.гггг."); }
    }
}
