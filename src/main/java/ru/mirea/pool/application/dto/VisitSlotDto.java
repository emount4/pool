package ru.mirea.pool.application.dto;

import java.time.LocalTime;
import java.util.List;

public record VisitSlotDto(
        LocalTime startTime,
        LocalTime endTime,
        List<Integer> availableLaneNumbers
) {

    public VisitSlotDto {
        availableLaneNumbers = List.copyOf(availableLaneNumbers);
    }
}
