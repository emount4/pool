package ru.mirea.pool.backend.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.Lane;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.repository.ClientRepository;
import ru.mirea.pool.backend.repository.LaneRepository;
import ru.mirea.pool.backend.repository.VisitRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VisitSlotService {

    private final VisitRepository visits;
    private final LaneRepository lanes;
    private final ClientRepository clients;
    private final VisitRules rules;

    public VisitSlotService(VisitRepository visits, LaneRepository lanes, ClientRepository clients, VisitRules rules) {
        this.visits = visits;
        this.lanes = lanes;
        this.clients = clients;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public List<VisitSlot> find(LocalDate date, int duration, long clientId, UUID except) {
        rules.validateDuration(duration);
        if (!clients.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Клиент не найден.");
        }
        List<Visit> booked = visits.findByVisitDateAndStatusNot(date, VisitStatus.CANCELLED).stream()
                .filter(visit -> !visit.getPublicId().equals(except)).toList();
        List<Lane> availableLanes = lanes.findAll(Sort.by("number")).stream().filter(Lane::isActive).toList();
        LocalDateTime now = rules.now();
        LocalDateTime closing = date.atTime(VisitRules.CLOSING_TIME);
        List<VisitSlot> slots = new ArrayList<>();
        for (LocalDateTime start = date.atTime(VisitRules.OPENING_TIME);
             !start.plusMinutes(duration).isAfter(closing);
             start = start.plusMinutes(VisitRules.SLOT_MINUTES)) {
            if (!start.isAfter(now)) {
                continue;
            }
            LocalTime startsAt = start.toLocalTime();
            LocalTime endsAt = start.plusMinutes(duration).toLocalTime();
            List<Visit> overlapping = booked.stream()
                    .filter(visit -> visit.getStartTime().isBefore(endsAt)
                            && visit.getStartTime().plusMinutes(visit.getDurationMinutes()).isAfter(startsAt))
                    .toList();
            if (overlapping.stream().anyMatch(visit -> visit.getClient().getId() == clientId)) {
                continue;
            }
            List<Integer> numbers = availableLanes.stream().map(Lane::getNumber)
                    .filter(number -> overlapping.stream().noneMatch(visit -> visit.getLane().getNumber().equals(number)))
                    .toList();
            if (!numbers.isEmpty()) {
                slots.add(new VisitSlot(startsAt, endsAt, numbers));
            }
        }
        return slots;
    }

    public record VisitSlot(LocalTime startTime, LocalTime endTime, List<Integer> laneNumbers) {
    }
}
