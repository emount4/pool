package ru.mirea.pool.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record VisitRequest(
        @NotNull @Positive Long clientId,
        @NotNull @Positive Integer laneNumber,
        @NotNull LocalDate visitDate,
        @NotNull LocalTime startTime,
        @NotNull @Min(30) @Max(180) Integer durationMinutes,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        boolean individual,
        @PositiveOrZero Long version
) {
}
