package ru.mirea.pool.presentation.fx.util;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** JDBC and file I/O run off the FX thread; callbacks always run on it. */
public final class FxTasks implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "pool-worker");
        thread.setDaemon(true);
        return thread;
    });

    public <T> void run(Node pane, Label status, String message, Callable<T> work, Consumer<T> success) {
        run(pane, status, message, work, success, error -> Ui.error(pane.getScene().getWindow(), error));
    }

    public <T> void run(Node pane, Label status, String message, Callable<T> work,
                        Consumer<T> success, Consumer<Throwable> failure) {
        pane.setDisable(true);
        if (status != null) status.setText(message);
        var task = new Task<T>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(event -> {
            pane.setDisable(false);
            if (status != null) status.setText("Готово");
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            pane.setDisable(false);
            if (status != null) status.setText("Не удалось выполнить операцию");
            failure.accept(task.getException());
        });
        executor.execute(task);
    }
    @Override public void close() { executor.shutdownNow(); }
}
