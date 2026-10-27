package io.jenkins.plugins.specsmonitor;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts a human-readable CPU model name and clock speed from Linux tool output. */
final class CpuNameParser {

    /** Keys in /proc/cpuinfo that hold a readable name, in order of preference. */
    private static final List<String> PROC_CPUINFO_KEYS = List.of("model name", "cpu", "hardware");

    /**
     * Keys in /proc/cpuinfo that hold the clock speed in MHz: x86 ({@code cpu MHz}) and POWER
     * ({@code clock}).
     */
    private static final List<String> PROC_CPUINFO_MHZ_KEYS = List.of("cpu mhz", "clock");

    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\d+(?:\\.\\d+)?");

    private CpuNameParser() {}

    /**
     * Parses the output of {@code lscpu} (run with {@code LC_ALL=C}) and returns
     * the first
     * {@code Model name:} value, or an empty string if there is none.
     */
    static String fromLscpu(String output) {
        return lscpuValue(output, "Model name:");
    }

    /**
     * Parses the output of {@code lscpu} and returns the {@code CPU max MHz:} value rounded to
     * whole MHz, or 0 if there is none (for example in most virtual machines).
     */
    static int maxMhzFromLscpu(String output) {
        return parseMhz(lscpuValue(output, "CPU max MHz:"));
    }

    /**
     * Parses the lines of {@code /proc/cpuinfo} and returns the clock speed of the first
     * processor in whole MHz, or 0 if there is none (for example on aarch64).
     */
    static int mhzFromProcCpuinfo(List<String> lines) {
        return parseMhz(procCpuinfoValue(lines, PROC_CPUINFO_MHZ_KEYS));
    }

    private static String lscpuValue(String output, String key) {
        if (output == null) {
            return "";
        }
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key)) {
                String value = trimmed.substring(key.length()).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    /** Parses a value such as {@code 5400.0000} or {@code 2950.000000MHz}. */
    private static int parseMhz(String value) {
        Matcher m = LEADING_NUMBER.matcher(value);
        return m.find() ? (int) Math.round(Double.parseDouble(m.group())) : 0;
    }

    /**
     * Parses the lines of {@code /proc/cpuinfo}. Works for x86 ({@code model name})
     * and
     * POWER ({@code cpu}). Architectures that only print numeric IDs, such as
     * aarch64, yield
     * an empty string.
     */
    static String fromProcCpuinfo(List<String> lines) {
        return procCpuinfoValue(lines, PROC_CPUINFO_KEYS);
    }

    private static String procCpuinfoValue(List<String> lines, List<String> keys) {
        for (String key : keys) {
            for (String line : lines) {
                int colon = line.indexOf(':');
                if (colon > 0
                        && line.substring(0, colon)
                                .trim()
                                .toLowerCase(Locale.ROOT)
                                .equals(key)) {
                    String value = line.substring(colon + 1).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return "";
    }
}
