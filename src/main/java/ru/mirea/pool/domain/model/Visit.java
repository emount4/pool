package ru.mirea.pool.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Getter
@Setter
public class Visit {

    private Long id;
    private Long clientId;
    private LocalDate visitDate;
    private LocalTime startTime;
    private int durationMinutes;
    private int laneNumber;
    private VisitStatus status;
    private Long createdByUserId;

    public Visit() {
    }

    public Visit(
            Long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            int laneNumber,
            VisitStatus status
    ) {
        this(null, clientId, visitDate, startTime, durationMinutes, laneNumber, status, null);
    }

    public Visit(
            Long id,
            Long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            int laneNumber,
            VisitStatus status
    ) {
        this(id, clientId, visitDate, startTime, durationMinutes, laneNumber, status, null);
    }

    public Visit(
            Long id,
            Long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            int laneNumber,
            VisitStatus status,
            Long createdByUserId
    ) {
        this.id = id;
        this.clientId = clientId;
        this.visitDate = visitDate;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.laneNumber = laneNumber;
        this.status = status;
        this.createdByUserId = createdByUserId;
    }

    public LocalTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }

    @Override
    public String toString() {
        return "Visit{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", visitDate=" + visitDate +
                ", startTime=" + startTime +
                ", durationMinutes=" + durationMinutes +
                ", laneNumber=" + laneNumber +
                ", status=" + status +
                ", createdByUserId=" + createdByUserId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Visit visit)) {
            return false;
        }
        return Objects.equals(id, visit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
