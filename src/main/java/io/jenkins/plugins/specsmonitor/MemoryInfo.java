package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.Locale;

/** Total physical memory (RAM) of a node. */
public class MemoryInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final double KIB = 1024.0;
    private static final String NOT_AVAILABLE = "N/A";

    private final long totalBytes;

    /**
     * @param totalBytes total physical memory in bytes, or a value {@code <= 0} if
     *                   unknown
     */
    public MemoryInfo(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Human-readable size using binary units, e.g. {@code 15.6 GiB}, or {@code N/A}
     * if unknown.
     */
    public String getDisplay() {
        return format(totalBytes);
    }

    static String format(long bytes) {
        if (bytes <= 0) {
            return NOT_AVAILABLE;
        }
        double mb = bytes / KIB / KIB;
        double gb = mb / KIB;
        if (gb >= KIB) {
            return trim(gb / KIB) + " TiB";
        }
        if (gb >= 1) {
            return trim(gb) + " GiB";
        }
        return trim(mb) + " MiB";
    }

    private static String trim(double value) {
        String s = String.format(Locale.ROOT, "%.1f", value);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
