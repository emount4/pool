package ru.mirea.pool.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public void setLaneNumber(int laneNumber) {
        this.laneNumber = laneNumber;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        this.status = status;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
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
