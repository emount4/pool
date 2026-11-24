package ru.mirea.pool.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.CsvPreview;
import ru.mirea.pool.backend.domain.CsvPreviewRow;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.dto.VisitRequest;
import ru.mirea.pool.backend.dto.VisitResponse;
import ru.mirea.pool.backend.filter.VisitFilter;
import ru.mirea.pool.backend.repository.ClientRepository;
import ru.mirea.pool.backend.repository.CsvPreviewRepository;
import ru.mirea.pool.backend.repository.CsvPreviewRowRepository;
import ru.mirea.pool.backend.repository.LaneRepository;
import ru.mirea.pool.backend.repository.VisitRepository;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CsvService {

    private static final String[] HEADERS = {"visitNumber", "clientId", "laneNumber", "visitDate",
            "startTime", "durationMinutes", "title", "description", "individual"};
    private final CsvPreviewRepository previews;
    private final CsvPreviewRowRepository previewRows;
    private final ObjectMapper mapper;
    private final VisitRepository visits;
    private final ClientRepository clients;
    private final LaneRepository lanes;
    private final VisitService service;
    private final VisitRules rules;
    private final Validator validator;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public CsvService(CsvPreviewRepository previews, CsvPreviewRowRepository previewRows,
                      ObjectMapper mapper, VisitRepository visits, ClientRepository clients,
                      LaneRepository lanes, VisitService service, VisitRules rules, Validator validator,
                      AuditService audit, PlatformTransactionManager manager) {
        this.previews = previews;
        this.previewRows = previewRows;
        this.mapper = mapper;
        this.visits = visits;
        this.clients = clients;
        this.lanes = lanes;
        this.service = service;
        this.rules = rules;
        this.validator = validator;
        this.audit = audit;
        this.transaction = new TransactionTemplate(manager);
    }

    @Transactional(readOnly = true)
    public String export(VisitFilter filter, String sort, boolean descending) {
        try {
            StringWriter output = new StringWriter();
            output.write('\uFEFF');
            try (CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
                for (Visit visit : visits.findAll(filter.specification(), VisitService.sortOrder(sort, descending))) {
                    printer.printRecord(visit.getVisitNumber(), visit.getClient().getId(), visit.getLane().getNumber(),
                            visit.getVisitDate(), visit.getStartTime(), visit.getDurationMinutes(), visit.getTitle(),
                            visit.getDescription(), visit.isIndividual());
                }
            }
            return output.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать CSV.", exception);
        }
    }

    @Transactional
    public Preview preview(UUID userId, String text, Mode mode) {
        UUID id = UUID.randomUUID();
        previews.saveAndFlush(new CsvPreview(id, userId, mode.name(), Instant.now().plus(1, ChronoUnit.HOURS)));
        List<RowResult> results = new ArrayList<>();
        Set<String> numbers = new HashSet<>();
        List<ImportRow> accepted = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
                .parse(new StringReader(text.replaceFirst("^\uFEFF", "")))) {
            if (!parser.getHeaderNames().equals(List.of(HEADERS))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заголовки CSV должны совпадать с экспортом.");
            }
            int number = 1;
            for (var record : parser) {
                if (++number > 501) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В одном файле допустимо до 500 записей.");
                }
                String error = null;
                ImportRow row = null;
                try {
                    if (!record.isConsistent()) {
                        throw new IllegalArgumentException("Неверное количество столбцов.");
                    }
                    String code = record.get("visitNumber").strip();
                    if (code.isBlank() || code.length() > 64 || !numbers.add(code)) {
                        throw new IllegalArgumentException("Номер записи пуст, слишком длинный или повторяется.");
                    }
                    String individual = record.get("individual");
                    if (!Set.of("true", "false").contains(individual)) {
                        throw new IllegalArgumentException("individual должен быть true или false.");
                    }
                    Visit existing = visits.findByVisitNumber(code).orElse(null);
                    if (existing != null && mode == Mode.NEW_ONLY) {
                        throw new IllegalArgumentException("Номер уже существует.");
                    }
                    VisitRequest request = new VisitRequest(Long.valueOf(record.get("clientId")),
                            Integer.valueOf(record.get("laneNumber")), LocalDate.parse(record.get("visitDate")),
                            LocalTime.parse(record.get("startTime")), Integer.valueOf(record.get("durationMinutes")),
                            record.get("title"), record.get("description"), Boolean.parseBoolean(individual),
                            existing == null ? null : existing.getVersion());
                    row = new ImportRow(code, request, existing == null ? null : existing.getPublicId());
                    validate(row);
                    for (ImportRow previous : accepted) {
                        if (overlaps(previous.request(), request)) {
                            throw new IllegalArgumentException("Пересечение с другой строкой файла.");
                        }
                    }
                    accepted.add(row);
                } catch (ResponseStatusException exception) {
                    error = exception.getReason();
                } catch (java.time.DateTimeException exception) {
                    error = "Неверный формат даты или времени.";
                } catch (IllegalArgumentException exception) {
                    error = "Проверьте значения строки. " + exception.getMessage();
                }
                if (error != null && error.length() > 1000) {
                    error = error.substring(0, 1000);
                }
                previewRows.save(new CsvPreviewRow(id, number, error == null ? json(row) : null, error));
                results.add(new RowResult(number, error == null, error));
            }
        } catch (IOException | java.io.UncheckedIOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось прочитать CSV.");
        }
        long valid = results.stream().filter(RowResult::success).count();
        return new Preview(id, valid, results.size() - valid, results);
    }

    public List<RowResult> apply(UUID userId, UUID previewId) {
        List<Integer> rows = transaction.execute(ignored -> {
            requirePreview(userId, previewId);
            return previewRows.findRowNumbers(previewId);
        });
        List<RowResult> results = new ArrayList<>();
        for (int number : rows) {
            try {
                results.add(transaction.execute(ignored -> applyRow(userId, previewId, number)));
            } catch (ResponseStatusException exception) {
                results.add(new RowResult(number, false, exception.getReason()));
            } catch (org.springframework.dao.DataAccessException exception) {
                results.add(new RowResult(number, false, "Конфликт данных. Проверьте занятость и номер записи."));
            }
        }
        return results;
    }

    private RowResult applyRow(UUID userId, UUID previewId, int number) {
        requirePreview(userId, previewId);
        CsvPreviewRow stored = previewRows.findByPreviewIdAndRowNumber(previewId, number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Строка предпросмотра не найдена."));
        if (stored.getError() != null) {
            return new RowResult(number, false, stored.getError());
        }
        if (stored.getResult() != null) {
            return new RowResult(number, true, "Уже применено");
        }
        ImportRow row;
        try {
            row = mapper.readValue(stored.getPayload(), ImportRow.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
        validate(row);
        VisitResponse response;
        if (row.existingId() == null) {
            if (visits.findByVisitNumber(row.visitNumber()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Номер появился после проверки файла.");
            }
            response = service.create(row.request());
            Visit created = visits.findByPublicId(response.id()).orElseThrow();
            created.assignVisitNumber(row.visitNumber());
            visits.flush();
        } else {
            response = service.update(row.existingId(), row.request());
        }
        stored.markApplied(response.id());
        audit.record("CSV_IMPORTED", "VISIT", response.id(), "Применена строка CSV");
        return new RowResult(number, true, null);
    }

    private void requirePreview(UUID userId, UUID previewId) {
        if (!previews.existsValid(previewId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Проверка файла не найдена или срок её действия истёк.");
        }
    }

    private void validate(ImportRow row) {
        VisitRequest request = row.request();
        if (!validator.validate(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Обязательные поля или диапазоны заполнены неверно.");
        }
        rules.validateSchedule(request.visitDate(), request.startTime(), request.durationMinutes());
        if (!clients.existsById(request.clientId())
                || lanes.findById(request.laneNumber()).filter(lane -> lane.isActive()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Клиент или действующая дорожка не найдены.");
        }
        if (row.existingId() != null) {
            Visit existing = visits.findByPublicId(row.existingId()).orElseThrow();
            rules.requireEditable(existing);
            if (existing.getVersion() != request.version()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Запись изменена после проверки файла.");
            }
        }
        for (Visit booked : visits.findByVisitDateAndStatusNot(request.visitDate(), VisitStatus.CANCELLED)) {
            if (booked.getPublicId().equals(row.existingId())) {
                continue;
            }
            if ((booked.getClient().getId().equals(request.clientId())
                    || booked.getLane().getNumber().equals(request.laneNumber()))
                    && booked.getStartTime().isBefore(request.startTime().plusMinutes(request.durationMinutes()))
                    && booked.getStartTime().plusMinutes(booked.getDurationMinutes()).isAfter(request.startTime())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Время клиента или дорожки занято.");
            }
        }
    }

    private boolean overlaps(VisitRequest first, VisitRequest second) {
        return first.visitDate().equals(second.visitDate())
                && (first.clientId().equals(second.clientId()) || first.laneNumber().equals(second.laneNumber()))
                && first.startTime().isBefore(second.startTime().plusMinutes(second.durationMinutes()))
                && second.startTime().isBefore(first.startTime().plusMinutes(first.durationMinutes()));
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public enum Mode {
        NEW_ONLY, UPSERT
    }

    public record ImportRow(String visitNumber, VisitRequest request, UUID existingId) {
    }

    public record RowResult(int row, boolean success, String error) {
    }

    public record Preview(UUID id, long validRows, long invalidRows, List<RowResult> rows) {
    }
}
