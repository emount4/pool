package ru.mirea.pool.backend.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisitPriorityTest {

    @Test
    void calculatesPriorityFromIndividualFlagAndDuration() {
        assertEquals(VisitPriority.STANDARD, VisitPriority.calculate(false, 180));
        assertEquals(VisitPriority.ELEVATED, VisitPriority.calculate(true, 60));
        assertEquals(VisitPriority.HIGH, VisitPriority.calculate(true, 90));
    }
}
