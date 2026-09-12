package ru.mirea.pool.application.service;

import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.domain.exception.AccessDeniedException;
import ru.mirea.pool.domain.exception.BusinessRuleException;
import ru.mirea.pool.domain.exception.EntityNotFoundException;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.domain.model.Visit;
import ru.mirea.pool.domain.model.VisitStatus;
import ru.mirea.pool.domain.repository.ClientRepository;
import ru.mirea.pool.domain.repository.VisitRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class VisitService {

    private static final int MINIMUM_DURATION_MINUTES = 30;
    private static final int MAXIMUM_DURATION_MINUTES = 180;
    private static final int MINIMUM_LANE_NUMBER = 1;
    private static final int MAXIMUM_LANE_NUMBER = 8;

    private static final Comparator<Visit> DATE_TIME_COMPARATOR = Comparator
            .comparing(Visit::getVisitDate)
            .thenComparing(Visit::getStartTime)
            .thenComparing(Visit::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;

    public VisitService(
            ClientRepository clientRepository,
            VisitRepository visitRepository
    ) {
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
    }

    public Visit createVisit(
            UserSession actor,
            long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            int laneNumber
    ) {
        requireAuthenticated(actor);
        validateVisitData(clientId, visitDate, startTime, durationMinutes, laneNumber);
        if (visitDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Новое посещение нельзя создать в прошлом.");
        }
        ensureNoOverlap(clientId, visitDate, startTime, durationMinutes, null);

        Visit visit = new Visit(
                null,
                clientId,
                visitDate,
                startTime,
                durationMinutes,
                laneNumber,
                VisitStatus.PLANNED,
                actor.userId()
        );
        return visitRepository.save(visit);
    }

    public Visit getVisitById(long visitId) {
        validateId(visitId);
        return findRequiredVisit(visitId);
    }

    public List<Visit> getAllVisits() {
        return visitRepository.findAll();
    }

    public Visit updateVisit(
            long visitId,
            long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            int laneNumber
    ) {
        validateId(visitId);
        Visit existing = findRequiredVisit(visitId);
        validateVisitData(clientId, visitDate, startTime, durationMinutes, laneNumber);
        ensureNoOverlap(clientId, visitDate, startTime, durationMinutes, visitId);

        existing.setClientId(clientId);
        existing.setVisitDate(visitDate);
        existing.setStartTime(startTime);
        existing.setDurationMinutes(durationMinutes);
        existing.setLaneNumber(laneNumber);
        visitRepository.update(existing);
        return existing;
    }

    public void deleteVisit(long visitId) {
        validateId(visitId);
        findRequiredVisit(visitId);
        visitRepository.deleteById(visitId);
    }

    public Visit changeStatus(long visitId, VisitStatus newStatus) {
        validateId(visitId);
        if (newStatus == null) {
            throw new ValidationException("Новый статус посещения обязателен.");
        }

        Visit visit = findRequiredVisit(visitId);
        if (!isAllowedTransition(visit.getStatus(), newStatus)) {
            throw new BusinessRuleException(
                    "Переход статуса " + visit.getStatus() + " -> " + newStatus + " запрещён."
            );
        }

        visit.setStatus(newStatus);
        visitRepository.update(visit);
        return visit;
    }

    public List<Visit> searchByClient(long clientId) {
        validateClientId(clientId);
        ensureClientExists(clientId);
        return visitRepository.findByClientId(clientId);
    }

    public List<Visit> searchByDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("Дата посещения обязательна.");
        }
        return visitRepository.findByDate(date);
    }

    public List<Visit> filterByStatus(VisitStatus status) {
        if (status == null) {
            throw new ValidationException("Статус для фильтрации обязателен.");
        }
        return visitRepository.findByStatus(status);
    }

    public List<Visit> filterByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Обе границы диапазона дат обязательны.");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("Начальная дата не может быть позже конечной.");
        }
        return visitRepository.findByDateRange(from, to);
    }

    public List<Visit> sortByDateAscending() {
        return visitRepository.findAll().stream()
                .sorted(DATE_TIME_COMPARATOR)
                .toList();
    }

    public List<Visit> sortByDateDescending() {
        return visitRepository.findAll().stream()
                .sorted(DATE_TIME_COMPARATOR.reversed())
                .toList();
    }

    public List<Visit> sortByDurationAscending() {
        return visitRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Visit::getDurationMinutes)
                        .thenComparing(DATE_TIME_COMPARATOR))
                .toList();
    }

    private void validateVisitData(
            long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            int laneNumber
    ) {
        validateClientId(clientId);
        ensureClientExists(clientId);

        if (visitDate == null) {
            throw new ValidationException("Дата посещения обязательна.");
        }
        if (startTime == null) {
            throw new ValidationException("Время начала посещения обязательно.");
        }
        if (durationMinutes < MINIMUM_DURATION_MINUTES
                || durationMinutes > MAXIMUM_DURATION_MINUTES) {
            throw new ValidationException("Продолжительность должна быть от 30 до 180 минут.");
        }
        if (laneNumber < MINIMUM_LANE_NUMBER || laneNumber > MAXIMUM_LANE_NUMBER) {
            throw new ValidationException("Номер дорожки должен быть от 1 до 8.");
        }
    }

    private void ensureNoOverlap(
            long clientId,
            LocalDate visitDate,
            LocalTime startTime,
            int durationMinutes,
            Long excludedVisitId
    ) {
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        boolean overlaps = visitRepository.findByClientIdAndDate(clientId, visitDate).stream()
                .filter(existing -> !Objects.equals(existing.getId(), excludedVisitId))
                .anyMatch(existing -> startTime.isBefore(existing.getEndTime())
                        && existing.getStartTime().isBefore(endTime));

        if (overlaps) {
            throw new BusinessRuleException(
                    "Посещение пересекается с другим посещением этого клиента."
            );
        }
    }

    private boolean isAllowedTransition(VisitStatus current, VisitStatus next) {
        return (current == VisitStatus.PLANNED
                && (next == VisitStatus.IN_PROGRESS || next == VisitStatus.CANCELLED))
                || (current == VisitStatus.IN_PROGRESS && next == VisitStatus.COMPLETED);
    }

    private Visit findRequiredVisit(long visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Посещение с ID " + visitId + " не найдено."
                ));
    }

    private void ensureClientExists(long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new EntityNotFoundException("Клиент с ID " + clientId + " не найден.");
        }
    }

    private void requireAuthenticated(UserSession actor) {
        if (actor == null) {
            throw new AccessDeniedException("Для создания посещения необходимо войти в систему.");
        }
    }

    private void validateClientId(long clientId) {
        if (clientId <= 0) {
            throw new ValidationException("ID клиента должен быть положительным.");
        }
    }

    private void validateId(long visitId) {
        if (visitId <= 0) {
            throw new ValidationException("ID посещения должен быть положительным.");
        }
    }
}
