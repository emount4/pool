package ru.mirea.pool.backend.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.Lane;
import ru.mirea.pool.backend.repository.LaneRepository;

import java.util.List;

@Service
public class LaneService {

    private final LaneRepository lanes;
    private final AuditService audit;

    public LaneService(LaneRepository lanes, AuditService audit) {
        this.lanes = lanes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<LaneResponse> list() {
        return lanes.findAll(Sort.by("number")).stream().map(LaneResponse::from).toList();
    }

    @Transactional
    public LaneResponse create(String name, boolean active) {
        Lane lane = lanes.saveAndFlush(new Lane(name.strip(), active));
        audit.record("LANE_CREATED", "LANE", lane.getPublicId(), "Добавлена дорожка " + lane.getNumber());
        return LaneResponse.from(lane);
    }

    @Transactional
    public LaneResponse update(int number, String name, boolean active) {
        Lane lane = lanes.findById(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Дорожка не найдена."));
        lane.update(name.strip(), active);
        audit.record("LANE_UPDATED", "LANE", lane.getPublicId(), "Изменена дорожка " + number);
        return LaneResponse.from(lane);
    }

    public record LaneResponse(int number, String name, boolean active) {

        static LaneResponse from(Lane lane) {
            return new LaneResponse(lane.getNumber(), lane.getName(), lane.isActive());
        }
    }
}
