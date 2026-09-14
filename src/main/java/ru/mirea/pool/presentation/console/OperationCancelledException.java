package ru.mirea.pool.presentation.console;

public final class OperationCancelledException extends RuntimeException {

    public OperationCancelledException() {
        super("Операция отменена.");
    }
}
