package com.example.inputbe.service;

import com.example.inputbe.dto.RecordCategory;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.exception.BadRequestException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class SheetsService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
