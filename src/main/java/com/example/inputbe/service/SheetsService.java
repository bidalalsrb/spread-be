package com.example.inputbe.service;

import com.example.inputbe.dto.AbsenceItemRequest;
import com.example.inputbe.dto.AbsenceItemResponse;
import com.example.inputbe.dto.RecordCategory;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.exception.BadRequestException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class SheetsService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ABSENCE_HEADER_DT = DateTimeFormatter.ofPattern("MMdd");
    private static final DateTimeFormatter ABSENCE_DATE_DT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${google.credentials.path:}")
    private String credentialsPath;

    @Value("${google.application.name:spread-input}")
    private String applicationName;

    public String appendMemo(AppSettings settings, RecordCategory category, String studentName, String content) {
        validateSettings(settings);
        String spreadsheetId = resolveSpreadsheetId(settings.getSpreadsheetId());

        String savedAt = ZonedDateTime.now(KST_ZONE).format(DT);
        List<List<Object>> values = Arrays.asList(
                Arrays.asList(studentName, content, savedAt)
        );

        ValueRange body = new ValueRange().setValues(values);
        String range = toRange(category.sheetName(), "A2:C");

        try {
            Sheets sheets = createClient();
            sheets.spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
            return savedAt;
        } catch (GoogleJsonResponseException e) {
            throw new BadRequestException("google sheets append failed: " + e.getDetails().getMessage());
        } catch (Exception e) {
            throw new BadRequestException("google sheets append failed: " + e.getMessage());
        }
    }

    public void testConnection(AppSettings settings) {
        validateSettings(settings);
        String spreadsheetId = resolveSpreadsheetId(settings.getSpreadsheetId());
        try {
            Sheets sheets = createClient();
            List<String> missingTabs = new ArrayList<>();

            for (RecordCategory category : RecordCategory.values()) {
                String range = toRange(category.sheetName(), "A1:C1");
                try {
                    sheets.spreadsheets().values().get(spreadsheetId, range).execute();
                } catch (GoogleJsonResponseException e) {
                    missingTabs.add(category.sheetName());
                }
            }

            if (!missingTabs.isEmpty()) {
                throw new BadRequestException("sheet connection failed: missing tabs - " + String.join(", ", missingTabs));
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (GoogleJsonResponseException e) {
            throw new BadRequestException("sheet connection failed: " + e.getDetails().getMessage());
        } catch (Exception e) {
            throw new BadRequestException("sheet connection failed: " + e.getMessage());
        }
    }

    public void appendAbsenceBatch(AppSettings settings, List<AbsenceItemRequest> items) {
        validateSettings(settings);
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("absence items are required");
        }

        String spreadsheetId = resolveSpreadsheetId(settings.getSpreadsheetId());
        LocalDate today = ZonedDateTime.now(KST_ZONE).toLocalDate();
        String headerLabel = today.format(ABSENCE_HEADER_DT);
        String dateLabel = today.format(ABSENCE_DATE_DT);

        String readRange = toRange(RecordCategory.ABSENCE.sheetName(), "A2:C");
        String tableRange = toRange(RecordCategory.ABSENCE.sheetName(), "A2:C10000");

        try {
            Sheets sheets = createClient();
            ValueRange existing = sheets.spreadsheets().values().get(spreadsheetId, readRange).execute();
            List<List<String>> rows = normalizeRows(existing.getValues());

            Map<String, String> mergedByStudent = new LinkedHashMap<>();
            int insertIndex = rows.size();
            boolean foundToday = false;

            List<List<String>> cleanedRows = new ArrayList<>();
            for (int i = 0; i < rows.size(); ) {
                List<String> row = rows.get(i);
                String a = row.get(0);
                String b = row.get(1);
                String c = row.get(2);

                if (isHeaderRow(a, b, c) && headerLabel.equals(a)) {
                    if (!foundToday) {
                        insertIndex = cleanedRows.size();
                        foundToday = true;
                    }

                    i++;
                    while (i < rows.size()) {
                        List<String> current = rows.get(i);
                        String ca = current.get(0);
                        String cb = current.get(1);
                        String cc = current.get(2);

                        if (isHeaderRow(ca, cb, cc)) {
                            break;
                        }
                        if (!cb.isBlank()) {
                            mergedByStudent.putIfAbsent(cb, cc);
                        }
                        i++;
                    }
                    continue;
                }

                cleanedRows.add(row);
                i++;
            }

            for (AbsenceItemRequest item : items) {
                String student = item.studentName().trim();
                String note = item.note().trim();
                if (!student.isBlank()) {
                    mergedByStudent.put(student, note);
                }
            }

            List<List<String>> todayBlock = new ArrayList<>();
            todayBlock.add(Arrays.asList(headerLabel, "", ""));
            int idx = 0;
            for (Map.Entry<String, String> entry : mergedByStudent.entrySet()) {
                String dateCell = idx == 0 ? dateLabel : "";
                todayBlock.add(Arrays.asList(dateCell, entry.getKey(), entry.getValue()));
                idx++;
            }
            todayBlock.add(Arrays.asList("", "", "총" + mergedByStudent.size() + "명"));

            List<List<String>> finalRows = new ArrayList<>(cleanedRows);
            finalRows.addAll(insertIndex, todayBlock);

            sheets.spreadsheets().values()
                    .clear(spreadsheetId, tableRange, new ClearValuesRequest())
                    .execute();

            ValueRange body = new ValueRange().setValues(castRows(finalRows));
            sheets.spreadsheets().values()
                    .update(spreadsheetId, readRange, body)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            throw new BadRequestException("google sheets append failed: " + e.getDetails().getMessage());
        } catch (Exception e) {
            throw new BadRequestException("google sheets append failed: " + e.getMessage());
        }
    }

    public String todayAbsenceDate() {
        return ZonedDateTime.now(KST_ZONE).toLocalDate().format(ABSENCE_DATE_DT);
    }

    public List<AbsenceItemResponse> getTodayAbsences(AppSettings settings) {
        validateSettings(settings);
        String spreadsheetId = resolveSpreadsheetId(settings.getSpreadsheetId());
        String todayHeader = ZonedDateTime.now(KST_ZONE).toLocalDate().format(ABSENCE_HEADER_DT);
        String todayDate = ZonedDateTime.now(KST_ZONE).toLocalDate().format(ABSENCE_DATE_DT);
        String range = toRange(RecordCategory.ABSENCE.sheetName(), "A2:C");

        try {
            Sheets sheets = createClient();
            ValueRange existing = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
            List<List<String>> rows = normalizeRows(existing.getValues());
            Map<String, String> byStudent = new LinkedHashMap<>();

            for (int i = 0; i < rows.size(); ) {
                List<String> row = rows.get(i);
                String a = row.get(0);
                String b = row.get(1);
                String c = row.get(2);

                if (isHeaderRow(a, b, c) && todayHeader.equals(a)) {
                    i++;
                    while (i < rows.size()) {
                        List<String> current = rows.get(i);
                        String ca = current.get(0);
                        String cb = current.get(1);
                        String cc = current.get(2);
                        if (isHeaderRow(ca, cb, cc)) {
                            break;
                        }
                        if (!cb.isBlank()) {
                            byStudent.put(cb, cc);
                        }
                        i++;
                    }
                    continue;
                }
                i++;
            }

            List<AbsenceItemResponse> items = new ArrayList<>();
            for (Map.Entry<String, String> entry : byStudent.entrySet()) {
                items.add(new AbsenceItemResponse(entry.getKey(), entry.getValue(), todayDate));
            }
            return items;
        } catch (GoogleJsonResponseException e) {
            throw new BadRequestException("google sheets read failed: " + e.getDetails().getMessage());
        } catch (Exception e) {
            throw new BadRequestException("google sheets read failed: " + e.getMessage());
        }
    }

    private List<List<String>> normalizeRows(List<List<Object>> rawRows) {
        List<List<String>> rows = new ArrayList<>();
        if (rawRows == null) {
            return rows;
        }
        for (List<Object> raw : rawRows) {
            String a = raw.size() > 0 ? String.valueOf(raw.get(0)).trim() : "";
            String b = raw.size() > 1 ? String.valueOf(raw.get(1)).trim() : "";
            String c = raw.size() > 2 ? String.valueOf(raw.get(2)).trim() : "";
            rows.add(Arrays.asList(a, b, c));
        }
        return rows;
    }

    private boolean isHeaderRow(String a, String b, String c) {
        return a.matches("\\d{4}") && b.isBlank() && c.isBlank();
    }

    private List<List<Object>> castRows(List<List<String>> rows) {
        List<List<Object>> casted = new ArrayList<>();
        for (List<String> row : rows) {
            casted.add(Arrays.asList(row.get(0), row.get(1), row.get(2)));
        }
        return casted;
    }

    private Sheets createClient() throws IOException, GeneralSecurityException {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new BadRequestException("google credentials path is required");
        }

        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credentials;
        try (InputStream in = openCredentialsStream(credentialsPath.trim())) {
            credentials = GoogleCredentials.fromStream(in)
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS));
        }

        return new Sheets.Builder(transport, JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }

    private void validateSettings(AppSettings settings) {
        if (settings.getSpreadsheetId() == null || settings.getSpreadsheetId().isBlank()) {
            throw new BadRequestException("spreadsheetId is not configured");
        }
    }

    private String resolveSpreadsheetId(String rawValue) {
        String value = rawValue.trim();
        if (!value.contains("/spreadsheets/d/")) {
            return value;
        }

        int start = value.indexOf("/spreadsheets/d/") + "/spreadsheets/d/".length();
        int end = value.indexOf('/', start);
        if (end == -1) {
            end = value.indexOf('?', start);
        }
        if (end == -1) {
            end = value.length();
        }
        return value.substring(start, end);
    }

    private String toRange(String sheetName, String cells) {
        String escaped = sheetName.replace("'", "''");
        return "'" + escaped + "'!" + cells;
    }

    private InputStream openCredentialsStream(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String classpathLocation = path.substring("classpath:".length());
            return new ClassPathResource(classpathLocation).getInputStream();
        }
        return new FileInputStream(path);
    }
}
