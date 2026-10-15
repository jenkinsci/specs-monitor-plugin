package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;

/** How long the machine of a node has been running. */
public class UptimeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final long MINUTE = 60;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private final long seconds;

    /** @param seconds uptime in seconds, or a negative value if unknown */
    public UptimeInfo(long seconds) {
        this.seconds = seconds;
    }

    public long getSeconds() {
        return seconds;
    }

    /** Compact uptime, e.g. {@code 3d 4h}, {@code 5h 12m}, {@code 7m}, or {@code N/A}. */
    public String getDisplay() {
        if (seconds < 0) {
            return "N/A";
        }
        long days = seconds / DAY;
        long hours = seconds % DAY / HOUR;
        long minutes = seconds % HOUR / MINUTE;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    /** Full uptime, e.g. {@code Up for 3 days, 4 hours, 12 minutes}. Empty if unknown. */
    public String getTooltip() {
        if (seconds < 0) {
            return "";
        }
        long days = seconds / DAY;
        long hours = seconds % DAY / HOUR;
        long minutes = seconds % HOUR / MINUTE;
        return "Up for " + plural(days, "day") + ", " + plural(hours, "hour") + ", " + plural(minutes, "minute");
    }

    private static String plural(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
