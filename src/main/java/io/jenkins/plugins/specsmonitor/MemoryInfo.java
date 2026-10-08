package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.Locale;

/** Total, free and available physical memory (RAM) of a node. */
public class MemoryInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final double KIB = 1024.0;
    private static final String NOT_AVAILABLE = "N/A";

    private final long totalBytes;
    private final long freeBytes;
    private final long availableBytes;

    /**
     * @param totalBytes total physical memory in bytes, or a value {@code <= 0} if
     *                   unknown
     */
    public MemoryInfo(long totalBytes) {
        this(totalBytes, -1, -1);
    }

    /**
     * @param totalBytes     total physical memory in bytes, or a value {@code <= 0}
     *                       if unknown
     * @param freeBytes      memory that is completely unused, or a negative value
     *                       if unknown
     * @param availableBytes memory that can be used by applications without
     *                       swapping (free memory plus
     *                       reclaimable caches), or a negative value if unknown
     */
    public MemoryInfo(long totalBytes, long freeBytes, long availableBytes) {
        this.totalBytes = totalBytes;
        this.freeBytes = freeBytes;
        this.availableBytes = availableBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getFreeBytes() {
        return freeBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    /**
     * Memory that applications can use right now: the available memory if known,
     * otherwise the free memory.
     */
    public long getUsableBytes() {
        return availableBytes >= 0 ? availableBytes : freeBytes;
    }

    /**
     * Human-readable available (or free) memory using binary units, e.g.
     * {@code 43.5 GiB}, or {@code N/A}.
     */
    public String getDisplay() {
        return format(getUsableBytes());
    }

    /**
     * Tooltip text, e.g. {@code 43.5 GiB free of 63.7 GiB}. If the operating system
     * reports different values for
     * available memory (usable without swapping, including reclaimable caches) and
     * free memory (completely
     * unused), both are shown, e.g.
     * {@code Available: 8.4 GiB of 15.6 GiB, Free: 1.2 GiB}. Empty if neither is
     * known.
     */
    public String getTooltip() {
        boolean hasTotal = totalBytes > 0;
        String ofTotal = hasTotal ? " of " + format(totalBytes) : "";
        boolean hasFree = freeBytes >= 0;
        boolean hasAvailable = availableBytes >= 0;
        if (hasFree && hasAvailable && !format(freeBytes).equals(format(availableBytes))) {
            return "Available: " + format(availableBytes) + ofTotal + ", Free: " + format(freeBytes);
        }
        if (hasAvailable) {
            return format(availableBytes) + " free" + ofTotal;
        }
        if (hasFree) {
            return format(freeBytes) + " free" + ofTotal;
        }
        return "";
    }

    static String format(long bytes) {
        if (bytes < 0) {
            return NOT_AVAILABLE;
        }
        double mib = bytes / KIB / KIB;
        double gib = mib / KIB;
        if (gib >= KIB) {
            return trim(gib / KIB) + " TiB";
        }
        if (gib >= 1) {
            return trim(gib) + " GiB";
        }
        return trim(mib) + " MiB";
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
