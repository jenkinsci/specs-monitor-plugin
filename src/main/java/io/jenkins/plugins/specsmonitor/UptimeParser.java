package io.jenkins.plugins.specsmonitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses uptime output of operating system tools. All methods return -1 if unknown. */
final class UptimeParser {

    private static final Pattern BOOT_SEC = Pattern.compile("sec\\s*=\\s*(\\d+)");

    private UptimeParser() {}

    /** Parses a whole or fractional number of seconds, e.g. {@code 12345.67}. */
    static long fromSeconds(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        try {
            double value = Double.parseDouble(text.trim().split("\\s+")[0]);
            return value < 0 ? -1 : (long) value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parses {@code sysctl -n kern.boottime} output such as
     * {@code { sec = 1697000000, usec = 0 } Mon Oct 11 ...} to an uptime.
     */
    static long fromMacBootTime(String text, long nowEpochSeconds) {
        if (text == null) {
            return -1;
        }
        Matcher m = BOOT_SEC.matcher(text);
        if (!m.find()) {
            return -1;
        }
        long uptime = nowEpochSeconds - Long.parseLong(m.group(1));
        return uptime < 0 ? -1 : uptime;
    }
}
