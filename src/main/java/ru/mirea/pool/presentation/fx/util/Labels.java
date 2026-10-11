package ru.mirea.pool.presentation.fx.util;

import ru.mirea.pool.domain.model.*;
import java.time.format.DateTimeFormatter;

public final class Labels {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private Labels() { }
    public static String client(Client c) { return c == null ? "Клиент не найден" : c.getLastName() + " " + c.getFirstName(); }
    public static String status(VisitStatus s) {
        if (s == null) return "Все статусы";
        return switch (s) {
            case PLANNED -> "Запланировано";
            case IN_PROGRESS -> "Идёт сейчас";
            case COMPLETED -> "Завершено";
            case CANCELLED -> "Отменено";
        };
    }
    public static String role(UserRole r) { return r == UserRole.ADMIN ? "Администратор" : "Оператор"; }
}
