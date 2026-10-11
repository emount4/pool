package ru.mirea.pool.infrastructure.export;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

/** Exports exactly the ordered, filtered rows supplied by the screen. */
public final class TableExporter {
    public void export(Path destination, List<String> headers, List<List<String>> rows) {
        Path temp = null;
        try {
            Path target = destination.toAbsolutePath();
            temp = Files.createTempFile(target.getParent(), ".pool-export-", ".tmp");
            if (target.toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) writeExcel(temp, headers, rows);
            else writeCsv(temp, headers, rows);
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) {
            throw new ExportException("Не удалось сохранить экспорт. Проверьте права на папку и закройте файл в Excel.", e);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); }
                catch (IOException e) { org.slf4j.LoggerFactory.getLogger(TableExporter.class).warn("Не удалось удалить временный файл экспорта", e); }
            }
        }
    }
    private void writeCsv(Path file, List<String> headers, List<List<String>> rows) throws IOException {
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // Excel recognises UTF-8 and Russian text.
            writer.write(csvRow(headers));
            for (var row : rows) writer.write(csvRow(row));
        }
    }
    private String csvRow(List<String> row) {
        return row.stream().map(this::safeCsv).map(s -> "\"" + s.replace("\"", "\"\"") + "\"")
                .collect(java.util.stream.Collectors.joining(";")) + "\r\n";
    }
    private String safeCsv(String value) {
        String s = value == null ? "" : value;
        String stripped = s.stripLeading();
        return !stripped.isEmpty() && "=+-@\t\r\n".indexOf(stripped.charAt(0)) >= 0 ? "'" + s : s;
    }
    private void writeExcel(Path file, List<String> headers, List<List<String>> rows) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Данные");
            var font = workbook.createFont(); font.setBold(true);
            var style = workbook.createCellStyle(); style.setFont(font);
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) { var cell = header.createCell(i); cell.setCellValue(headers.get(i)); cell.setCellStyle(style); }
            for (int r = 0; r < rows.size(); r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < rows.get(r).size(); c++) row.createCell(c).setCellValue(rows.get(r).get(c));
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, headers.size() - 1));
            for (int c = 0; c < headers.size(); c++) sheet.setColumnWidth(c, (c == 0 ? 30 : 24) * 256);
            try (var output = Files.newOutputStream(file)) { workbook.write(output); }
        }
    }
}
