package ru.mirea.pool;

import ru.mirea.pool.presentation.fx.PoolApplication;

/** Separate launcher is also compatible with a classpath distribution. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        PoolApplication.main(args);
    }
}
