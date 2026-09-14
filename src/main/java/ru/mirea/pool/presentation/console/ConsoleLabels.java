package ru.mirea.pool.presentation.console;

import ru.mirea.pool.domain.model.VisitStatus;

final class ConsoleLabels {

    private ConsoleLabels() {
    }

    static String visitStatus(VisitStatus status) {
        return switch (status) {
            case PLANNED -> "Запланировано";
            case IN_PROGRESS -> "В процессе";
            case COMPLETED -> "Завершено";
            case CANCELLED -> "Отменено";
        };
    }
}
