package io.jenkins.plugins.specsmonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CpuScoreLookup {
    private static final Logger LOGGER = Logger.getLogger(CpuScoreLookup.class.getName());
    private static final Map<String, BigDecimal> SCORES = loadScores();

    private CpuScoreLookup() {}

    static String formatFor(String displayName) {
        if (displayName == null) {
            return null;
        }
        BigDecimal score = SCORES.get(normalizeName(displayName));
        return score == null ? null : formatScore(score);
    }

    private static String normalizeName(String displayName) {
        return displayName.toLowerCase(Locale.ROOT);
    }

    static String formatScore(BigDecimal score) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(score);
    }

    static Map<String, BigDecimal> parseCsv(Reader source) throws IOException {
        Map<String, BigDecimal> scores = new HashMap<>();
        BufferedReader reader = source instanceof BufferedReader ? (BufferedReader) source : new BufferedReader(source);
        try (reader) {
            String headerLine = reader.readLine();
            List<String> headers = headerLine == null ? null : parseRow(headerLine);
            if (headers == null) {
                return Map.of();
            }
            int deviceNameIndex = findColumn(headers, "Device Name");
            int medianScoreIndex = findColumn(headers, "Median Score");
            if (deviceNameIndex < 0 || medianScoreIndex < 0) {
                return Map.of();
            }
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = parseRow(line);
                if (fields == null || fields.size() <= Math.max(deviceNameIndex, medianScoreIndex)) {
                    continue;
                }
                String displayName = new CpuInfo(fields.get(deviceNameIndex).trim(), 0).getShortName();
                try {
                    BigDecimal score =
                            new BigDecimal(fields.get(medianScoreIndex).trim());
                    if (!displayName.isEmpty() && score.signum() >= 0) {
                        scores.putIfAbsent(normalizeName(displayName), score);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore invalid rows so one bad score does not disable the lookup.
                }
            }
        }
        return Map.copyOf(scores);
    }

    private static int findColumn(List<String> headers, String name) {
        for (int i = 0; i < headers.size(); i++) {
            if (name.equalsIgnoreCase(headers.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> parseRow(String line) {
        List<String> fields = new ArrayList<>(2);
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            return null;
        }
        fields.add(field.toString());
        return fields;
    }

    private static Map<String, BigDecimal> loadScores() {
        try (InputStream stream = CpuScoreLookup.class.getResourceAsStream("SpecsMonitor/blender_open_data.csv")) {
            if (stream == null) {
                return Map.of();
            }
            return parseCsv(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read CPU scores", e);
            return Map.of();
        }
    }
}
