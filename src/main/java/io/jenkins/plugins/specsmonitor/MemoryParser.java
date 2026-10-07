package io.jenkins.plugins.specsmonitor;

import java.util.List;

/** Extracts the total memory (RAM) in bytes from system tool output. */
final class MemoryParser {

    private static final String MEMTOTAL = "MemTotal:";

    private MemoryParser() {}

    /**
     * Parses a plain number of bytes, as printed by {@code sysctl -n hw.memsize} on
     * macOS or by
     * PowerShell's {@code TotalPhysicalMemory}. Returns -1 if the output is not a
     * positive number.
     */
    static long fromBytes(String output) {
        if (output == null) {
            return -1;
        }
        String firstLine = output.strip().lines().findFirst().orElse("").strip();
        try {
            long value = Long.parseLong(firstLine);
            return value > 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parses the lines of {@code /proc/meminfo} (the {@code MemTotal} value is in
     * kB) and returns
     * bytes, or -1 if it cannot be found.
     */
    static long fromMeminfo(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith(MEMTOTAL)) {
                String[] parts = line.substring(MEMTOTAL.length()).trim().split("\\s+");
                try {
                    long kib = Long.parseLong(parts[0]);
                    return kib > 0 ? kib * 1024 : -1;
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }
}
