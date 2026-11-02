package com.example.gstrecon.service;

import com.example.gstrecon.model.InvoiceRecord;
import com.example.gstrecon.util.NormalizerUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ExcelReaderService {
    private static final List<String> HEADERS = List.of("gstin", "supplier name", "invoice number", "invoice date", "taxable value", "igst", "cgst", "sgst");
    public List<InvoiceRecord> read(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Uploaded file is empty");
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        if (name.toLowerCase(Locale.ROOT).endsWith(".csv")) return readCsv(file);
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw new IllegalArgumentException("Only .xlsx and .csv files are supported");
        List<InvoiceRecord> records = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw new IllegalArgumentException("Workbook has no sheets");
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("First sheet is missing its header row");
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (int c = 0; c < HEADERS.size(); c++) {
                String actual = formatter.formatCellValue(header.getCell(c)).trim().toLowerCase(Locale.ROOT);
                if (!actual.equals(HEADERS.get(c))) throw new IllegalArgumentException("Expected column " + (c + 1) + " to be '" + HEADERS.get(c) + "'");
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i); if (row == null || isBlank(row, formatter)) continue;
                int rowNum = i + 1;
                try {
                    records.add(new InvoiceRecord(rowNum, NormalizerUtil.normalizeGstin(text(row, 0, formatter)),
                            text(row, 1, formatter), text(row, 2, formatter), date(row.getCell(3), formatter),
                            amount(row, 4, formatter), amount(row, 5, formatter), amount(row, 6, formatter), amount(row, 7, formatter)));
                } catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid value in row " + rowNum + ": " + ex.getMessage()); }
            }
        } catch (org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException ex) {
            throw new IllegalArgumentException("The uploaded file is not a valid .xlsx workbook");
        }
        if (records.isEmpty()) throw new IllegalArgumentException("No invoice rows were found after the header");
        return records;
    }
    private List<InvoiceRecord> readCsv(MultipartFile file) throws IOException {
        List<InvoiceRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("CSV file is empty");
            List<String> header = parseCsvLine(headerLine);
            if (header.size() != HEADERS.size()) throw new IllegalArgumentException("CSV header must contain exactly 8 required columns");
            for (int c = 0; c < HEADERS.size(); c++) {
                if (!header.get(c).trim().toLowerCase(Locale.ROOT).replace("\ufeff", "").equals(HEADERS.get(c)))
                    throw new IllegalArgumentException("Expected CSV column " + (c + 1) + " to be '" + HEADERS.get(c) + "'");
            }
            String line; int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                List<String> row = parseCsvLine(line);
                if (row.size() != HEADERS.size()) throw new IllegalArgumentException("CSV row " + rowNum + " must contain exactly 8 values");
                try {
                    records.add(new InvoiceRecord(rowNum, NormalizerUtil.normalizeGstin(row.get(0)), row.get(1).trim(), row.get(2).trim(),
                            parseTextDate(row.get(3).trim()), csvAmount(row.get(4)), csvAmount(row.get(5)), csvAmount(row.get(6)), csvAmount(row.get(7))));
                } catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid value in CSV row " + rowNum + ": " + ex.getMessage()); }
            }
        }
        if (records.isEmpty()) throw new IllegalArgumentException("No invoice rows were found after the header");
        return records;
    }
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>(); StringBuilder field = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == ',' && !quoted) { fields.add(field.toString()); field.setLength(0); }
            else field.append(ch);
        }
        if (quoted) throw new IllegalArgumentException("CSV row has an unclosed quoted field");
        fields.add(field.toString()); return fields;
    }
    private BigDecimal csvAmount(String value) { String s=value.trim().replace(",", "").replace("₹", ""); return s.isEmpty()?BigDecimal.ZERO:new BigDecimal(s); }
    private LocalDate parseTextDate(String value) {
        if (value.isBlank()) return null;
        String normalized = value.trim();
        int timeStart = normalized.indexOf(' ');
        if (timeStart > 0 && normalized.substring(timeStart).matches("\\s+\\d{1,2}:.*")) normalized = normalized.substring(0, timeStart);
        int timeZone = normalized.indexOf('T');
        if (timeZone > 0) normalized = normalized.substring(0, timeZone);
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d-M-uuuu"), DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("M-d-uuuu"), DateTimeFormatter.ofPattern("M/d/uuuu"),
                DateTimeFormatter.ofPattern("d.M.uuuu"), DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d-M-yy"), DateTimeFormatter.ofPattern("d/M/yy"),
                DateTimeFormatter.ofPattern("M-d-yy"), DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("d.M.yy"));
        for (DateTimeFormatter fmt : formats) {
            try { return LocalDate.parse(normalized, fmt); } catch (DateTimeParseException ignored) { }
        }
        // Excel sometimes exports a formatted CSV date as its serial day number.
        if (normalized.matches("\\d{4,5}")) {
            try { return LocalDate.of(1899, 12, 30).plusDays(Long.parseLong(normalized)); }
            catch (RuntimeException ignored) { }
        }
        return null;
    }
    private boolean isBlank(Row row, DataFormatter f) { for (int i=0;i<8;i++) if (!text(row,i,f).isBlank()) return false; return true; }
    private String text(Row row, int col, DataFormatter f) { Cell cell=row.getCell(col); return cell == null ? "" : f.formatCellValue(cell).trim(); }
    private BigDecimal amount(Row row, int col, DataFormatter f) {
        String s=text(row,col,f).replace(",", "").replace("₹", "").trim();
        return s.isEmpty() ? BigDecimal.ZERO : new BigDecimal(s);
    }
    private LocalDate date(Cell cell, DataFormatter f) {
        if (cell == null || f.formatCellValue(cell).isBlank()) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return parseTextDate(f.formatCellValue(cell).trim());
    }
}
