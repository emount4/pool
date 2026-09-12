package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.AuthenticationException;
import ru.mirea.pool.domain.exception.AccessDeniedException;
import ru.mirea.pool.domain.exception.BusinessRuleException;
import ru.mirea.pool.domain.exception.EntityNotFoundException;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.infrastructure.exception.DatabaseException;
import ru.mirea.pool.infrastructure.export.ExportException;

public final class ConsoleErrorHandler {

    public void handle(RuntimeException exception) {
        if (exception instanceof AuthenticationException
                || exception instanceof AccessDeniedException
                || exception instanceof BusinessRuleException
                || exception instanceof EntityNotFoundException
                || exception instanceof ValidationException) {
            System.out.println("Ошибка: " + exception.getMessage());
            return;
        }

        if (exception instanceof DatabaseException) {
            System.out.println("Ошибка работы с базой данных. Проверьте PostgreSQL и настройки подключения.");
            return;
        }

        if (exception instanceof ExportException) {
            System.out.println("Ошибка экспорта данных: " + exception.getMessage());
            return;
        }

        System.out.println("Непредвиденная ошибка. Операция не выполнена.");
    }
}
