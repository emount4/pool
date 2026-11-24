package ru.mirea.pool.backend.domain;



public enum VisitPriority {
    STANDARD,
    ELEVATED,
    HIGH;

    public static final int HIGH_PRIORITY_DURATION = 90;

    public static VisitPriority calculate(boolean individual, int durationMinutes) {
        if (!individual) {
            return STANDARD;
        }
        if (durationMinutes >= HIGH_PRIORITY_DURATION) {
            return HIGH;
        }
        return ELEVATED;
    }
}
