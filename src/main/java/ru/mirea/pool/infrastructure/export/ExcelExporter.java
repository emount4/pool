package ru.mirea.pool.infrastructure.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.domain.model.Visit;
import ru.mirea.pool.domain.model.VisitStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ExcelExporter implements DataExporter {

    private static final String[] CLIENT_HEADERS = {
            "ID", "First name", "Last name", "Phone", "Email", "Birth date"
    };

    private static final String[] VISIT_HEADERS = {
            "ID", "Client ID", "Visit date", "Start time", "Duration, min",
            "Lane", "Status", "Created by user ID"
    };

    @Override
    public void export(List<Client> clients, List<Visit> visits, Path destination) {
        Objects.requireNonNull(clients, "clients");
        Objects.requireNonNull(visits, "visits");
        Objects.requireNonNull(destination, "destination");

        try {
            createParentDirectory(destination);
            try (Workbook workbook = new XSSFWorkbook()) {
                CellStyle headerStyle = createHeaderStyle(workbook);
                writeClientsSheet(workbook, clients, headerStyle);
                writeVisitsSheet(workbook, visits, headerStyle);
                writeStatisticsSheet(workbook, clients, visits, headerStyle);

                try (OutputStream output = Files.newOutputStream(destination)) {
                    workbook.write(output);
                }
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось экспортировать данные в Excel.", e);
        }
    }

    private void writeClientsSheet(
            Workbook workbook,
            List<Client> clients,
            CellStyle headerStyle
    ) {
        Sheet sheet = workbook.createSheet("Clients");
        writeHeader(sheet, CLIENT_HEADERS, headerStyle);

        int rowIndex = 1;
        for (Client client : clients) {
            Row row = sheet.createRow(rowIndex++);
            setNullableLong(row.createCell(0), client.getId());
            row.createCell(1).setCellValue(safeText(client.getFirstName()));
            row.createCell(2).setCellValue(safeText(client.getLastName()));
            row.createCell(3).setCellValue(safeText(client.getPhone()));
            row.createCell(4).setCellValue(safeText(client.getEmail()));
            row.createCell(5).setCellValue(asText(client.getBirthDate()));
        }

        finishSheet(sheet, CLIENT_HEADERS.length);
    }

    private void writeVisitsSheet(
            Workbook workbook,
            List<Visit> visits,
            CellStyle headerStyle
    ) {
        Sheet sheet = workbook.createSheet("Visits");
        writeHeader(sheet, VISIT_HEADERS, headerStyle);

        int rowIndex = 1;
        for (Visit visit : visits) {
            Row row = sheet.createRow(rowIndex++);
            setNullableLong(row.createCell(0), visit.getId());
            setNullableLong(row.createCell(1), visit.getClientId());
            row.createCell(2).setCellValue(asText(visit.getVisitDate()));
            row.createCell(3).setCellValue(asText(visit.getStartTime()));
            row.createCell(4).setCellValue(visit.getDurationMinutes());
            row.createCell(5).setCellValue(visit.getLaneNumber());
            row.createCell(6).setCellValue(visit.getStatus().name());
            setNullableLong(row.createCell(7), visit.getCreatedByUserId());
        }

        finishSheet(sheet, VISIT_HEADERS.length);
    }

    private void writeStatisticsSheet(
            Workbook workbook,
            List<Client> clients,
            List<Visit> visits,
            CellStyle headerStyle
    ) {
        Sheet sheet = workbook.createSheet("Statistics");
        writeHeader(sheet, new String[]{"Metric", "Value"}, headerStyle);

        int rowIndex = 1;
        rowIndex = writeMetric(sheet, rowIndex, "Total clients", clients.size());
        rowIndex = writeMetric(sheet, rowIndex, "Total visits", visits.size());

        Map<VisitStatus, Long> statusCounts = visits.stream()
                .collect(Collectors.groupingBy(
                        Visit::getStatus,
                        () -> new java.util.EnumMap<>(VisitStatus.class),
                        Collectors.counting()
                ));
        for (VisitStatus status : VisitStatus.values()) {
            rowIndex = writeMetric(
                    sheet,
                    rowIndex,
                    status.name(),
                    statusCounts.getOrDefault(status, 0L)
            );
        }

        long visitsToday = visits.stream()
                .filter(visit -> LocalDate.now().equals(visit.getVisitDate()))
                .count();
        rowIndex = writeMetric(sheet, rowIndex, "Visits today", visitsToday);

        double averageDuration = visits.stream()
                .mapToInt(Visit::getDurationMinutes)
                .average()
                .orElse(0.0);
        rowIndex = writeMetric(sheet, rowIndex, "Average duration, min", averageDuration);

        Integer popularLane = findMostPopularLane(visits);
        writeMetric(sheet, rowIndex, "Most popular lane", popularLane);
        finishSheet(sheet, 2);
    }

    private Integer findMostPopularLane(List<Visit> visits) {
        return visits.stream()
                .map(Visit::getLaneNumber)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<Integer, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private int writeMetric(Sheet sheet, int rowIndex, String name, Number value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(name);
        if (value != null) {
            row.createCell(1).setCellValue(value.doubleValue());
        }
        return rowIndex + 1;
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        for (int column = 0; column < headers.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void finishSheet(Sheet sheet, int columnCount) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0,
                Math.max(0, sheet.getLastRowNum()),
                0,
                columnCount - 1
        ));
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
        }
    }

    private void createParentDirectory(Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void setNullableLong(Cell cell, Long value) {
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private String asText(Object value) {
        return value == null ? "" : value.toString();
    }
}
