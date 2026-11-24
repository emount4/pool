package ru.mirea.pool.backend.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.Client;
import ru.mirea.pool.backend.domain.Lane;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.dto.PageResponse;
import ru.mirea.pool.backend.dto.VisitRequest;
import ru.mirea.pool.backend.dto.VisitResponse;
import ru.mirea.pool.backend.filter.VisitFilter;
import ru.mirea.pool.backend.repository.ClientRepository;
import ru.mirea.pool.backend.repository.LaneRepository;
import ru.mirea.pool.backend.repository.VisitRepository;

import java.util.Set;
import java.util.UUID;

@Service
public class VisitService {

    private static final Set<String> SORT_FIELDS = Set.of("visitDate", "durationMinutes", "visitNumber");

    private final VisitRepository visits;
    private final ClientRepository clients;
    private final LaneRepository lanes;
    private final VisitRules rules;
    private final AuditService audit;

    public VisitService(VisitRepository visits, ClientRepository clients, LaneRepository lanes,
                        VisitRules rules, AuditService audit) {
        this.visits = visits;
        this.clients = clients;
        this.lanes = lanes;
        this.rules = rules;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public VisitResponse find(UUID id) {
        return VisitResponse.from(requireVisit(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitResponse> list(VisitFilter filter, int page, int size, String sort, boolean descending) {
        if (page < 0 || size < 1 || size > 100 || !SORT_FIELDS.contains(sort)
                || (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Проверьте параметры списка посещений.");
        }
        return PageResponse.from(visits.findAll(filter.specification(),
                        PageRequest.of(page, size, sortOrder(sort, descending)))
                .map(VisitResponse::from));
    }

    public static Sort sortOrder(String field, boolean descending) {
        if (!SORT_FIELDS.contains(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недопустимое поле сортировки.");
        }
        Sort.Direction direction = descending ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort order = Sort.by(direction, field);
        if ("visitDate".equals(field)) {
            order = order.and(Sort.by(direction, "startTime"));
        }
        return order.and(Sort.by("id"));
    }

    @Transactional
    public VisitResponse create(VisitRequest request) {
        rules.validateSchedule(request.visitDate(), request.startTime(), request.durationMinutes());
        Visit visit = new Visit(requireClient(request.clientId()), requireLane(request.laneNumber()),
                request.visitDate(), request.startTime(), request.durationMinutes(), request.title().strip(),
                description(request), request.individual());
        visits.saveAndFlush(visit);
        audit.record("VISIT_CREATED", "VISIT", visit.getPublicId(), "Создано посещение");
        return VisitResponse.from(visit);
    }

    @Transactional
    public VisitResponse update(UUID id, VisitRequest request) {
        Visit visit = requireVisit(id);
        if (request.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите версию посещения.");
        }
        if (request.version() != visit.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Посещение уже изменено. Обновите карточку.");
        }
        rules.requireEditable(visit);
        rules.validateSchedule(request.visitDate(), request.startTime(), request.durationMinutes());
        visit.reschedule(requireClient(request.clientId()), requireLane(request.laneNumber()),
                request.visitDate(), request.startTime(), request.durationMinutes(), request.title().strip(),
                description(request), request.individual());
        visits.flush();
        audit.record("VISIT_UPDATED", "VISIT", id, "Изменено посещение");
        return VisitResponse.from(visit);
    }

    @Transactional
    public void archive(UUID id) {
        requireVisit(id).archive();
        audit.record("VISIT_ARCHIVED", "VISIT", id, "Посещение перемещено в архив");
    }

    @Transactional
    public VisitResponse restore(UUID id) {
        Visit visit = requireVisit(id);
        visit.restore();
        audit.record("VISIT_RESTORED", "VISIT", id, "Посещение восстановлено");
        visits.flush();
        return VisitResponse.from(visit);
    }

    @Transactional
    public VisitResponse cancel(UUID id) {
        Visit visit = requireVisit(id);
        rules.requireEditable(visit);
        visit.changeStatus(VisitStatus.CANCELLED);
        audit.record("VISIT_CANCELLED", "VISIT", id, "Посещение отменено");
        visits.flush();
        return VisitResponse.from(visit);
    }

    @Transactional
    public void permanentlyDelete(UUID id) {
        Visit visit = requireVisit(id);
        if (!visit.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сначала архивируйте посещение.");
        }
        visits.delete(visit);
        audit.record("VISIT_DELETED", "VISIT", id, "Посещение окончательно удалено");
    }

    private Visit requireVisit(UUID id) {
        return visits.findByPublicId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Посещение не найдено."));
    }

    private Client requireClient(long id) {
        return clients.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клиент не найден."));
    }

    private Lane requireLane(int number) {
        return lanes.findById(number).filter(Lane::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите действующую дорожку."));
    }

    private String description(VisitRequest request) {
        return request.description() == null ? "" : request.description().strip();
    }
}
