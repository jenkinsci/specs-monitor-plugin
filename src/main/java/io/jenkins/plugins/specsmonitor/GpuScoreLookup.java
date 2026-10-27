package io.jenkins.plugins.specsmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GpuScoreLookup {
    private static final Logger LOGGER = Logger.getLogger(GpuScoreLookup.class.getName());
    private static final String RESOURCE_NAME = "GpuMonitor/blender_open_data.csv";
    private static final Pattern BRACKETED = Pattern.compile("\\[([^\\]]+)]\\s*$");
    private static final Pattern BRAND = Pattern.compile("(?i)^(?:(?:NVIDIA|AMD|Intel)\\s+)?(?:GeForce|Radeon)\\s+");

    private static final Map<String, BigDecimal> BY_FULL_NAME = load(GpuScoreLookup::fullKey);
    private static final Map<String, BigDecimal> BY_SHORT_NAME = load(GpuScoreLookup::shortKey);

    private GpuScoreLookup() {}

    static String formatFor(String name, String shortName) {
        if (name == null) {
            return null;
        }
        BigDecimal score = BY_FULL_NAME.get(fullKey(name));
        Matcher bracketed = BRACKETED.matcher(name);
        if (score == null && bracketed.find()) {
            score = BY_FULL_NAME.get(fullKey("NVIDIA " + bracketed.group(1)));
        }
        if (score == null && shortName != null) {
            score = BY_SHORT_NAME.get(CpuScoreLookup.normalizeName(shortName));
        }
        return score == null ? null : CpuScoreLookup.formatScore(score);
    }

    private static String fullKey(String name) {
        return CpuScoreLookup.normalizeName(name.replaceAll("\\s+", " ").trim());
    }

    /**
     * Key for the short-name fallback. Only rows whose name is just a brand-stripped model (for example
     * "NVIDIA GeForce RTX 4090") are used, so variants such as "RTX 5090 D" cannot claim the plain model's score.
     */
    private static String shortKey(String name) {
        String stripped = BRAND.matcher(fullKey(name)).replaceFirst("");
        GpuInfo info = new GpuInfo(stripped);
        return CpuScoreLookup.normalizeName(info.getShortName()).equals(CpuScoreLookup.normalizeName(stripped))
                ? CpuScoreLookup.normalizeName(stripped)
                : null;
    }

    static Map<String, BigDecimal> parseCsv(Reader source, java.util.function.UnaryOperator<String> keyFunction)
            throws IOException {
        return CpuScoreLookup.parseCsv(source, keyFunction);
    }

    private static Map<String, BigDecimal> load(java.util.function.UnaryOperator<String> keyFunction) {
        try (InputStream stream = GpuScoreLookup.class.getResourceAsStream(RESOURCE_NAME)) {
            if (stream == null) {
                return Map.of();
            }
            return parseCsv(new InputStreamReader(stream, StandardCharsets.UTF_8), keyFunction);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read GPU scores from " + RESOURCE_NAME, e);
            return Map.of();
        }
    }
}
