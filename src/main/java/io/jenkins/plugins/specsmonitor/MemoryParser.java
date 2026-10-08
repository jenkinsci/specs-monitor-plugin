package io.jenkins.plugins.specsmonitor;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts memory sizes in bytes from system tool output. Unknown values are
 * reported as -1.
 */
final class MemoryParser {

    private static final Pattern VM_STAT_PAGE_SIZE = Pattern.compile("page size of (\\d+) bytes");
    private static final Pattern VM_STAT_PAGES = Pattern.compile("^Pages (free|inactive|speculative):\\s+(\\d+)\\.?");

    private MemoryParser() {}

    /**
     * Parses a plain number of bytes, as printed by {@code sysctl -n hw.memsize} on
     * macOS.
     * Returns -1 if the output is not a positive number.
     */
    static long fromBytes(String output) {
        long[] values = fromBytesList(output, 1);
        return values[0] > 0 ? values[0] : -1;
    }

    /**
     * Parses up to {@code count} whitespace-separated byte values from the first
     * line of the output.
     * Missing or invalid values are returned as -1.
     */
    static long[] fromBytesList(String output, int count) {
        long[] result = new long[count];
        Arrays.fill(result, -1);
        if (output == null) {
            return result;
        }
        String[] parts = output.strip().lines().findFirst().orElse("").strip().split("\\s+");
        for (int i = 0; i < count && i < parts.length; i++) {
            try {
                long value = Long.parseLong(parts[i]);
                result[i] = value >= 0 ? value : -1;
            } catch (NumberFormatException e) {
                result[i] = -1;
            }
        }
        return result;
    }

    /**
     * Parses the {@code MemTotal} value of {@code /proc/meminfo} (in kB) and
     * returns bytes, or -1 if it
     * cannot be found or is not positive.
     */
    static long fromMeminfo(List<String> lines) {
        long total = fromMeminfoKey(lines, "MemTotal");
        return total > 0 ? total : -1;
    }

    /**
     * Returns the value of the given {@code /proc/meminfo} key (in kB) as bytes, or
     * -1 if it is missing.
     */
    static long fromMeminfoKey(List<String> lines, String key) {
        String prefix = key + ":";
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                String[] parts = line.substring(prefix.length()).trim().split("\\s+");
                try {
                    long kib = Long.parseLong(parts[0]);
                    return kib >= 0 ? kib * 1024 : -1;
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Parses the output of macOS {@code vm_stat} and returns
     * {@code {free, available}} in bytes. Available
     * memory is approximated as free + inactive + speculative pages. Returns
     * {@code {-1, -1}} on failure.
     */
    static long[] fromVmStat(String output) {
        long[] unknown = {-1, -1};
        if (output == null) {
            return unknown;
        }
        long pageSize = -1;
        long free = -1;
        long inactive = 0;
        long speculative = 0;
        for (String line : output.split("\\R")) {
            Matcher size = VM_STAT_PAGE_SIZE.matcher(line);
            if (size.find()) {
                pageSize = Long.parseLong(size.group(1));
                continue;
            }
            Matcher pages = VM_STAT_PAGES.matcher(line.strip());
            if (pages.find()) {
                long count = Long.parseLong(pages.group(2));
                switch (pages.group(1)) {
                    case "free" -> free = count;
                    case "inactive" -> inactive = count;
                    default -> speculative = count;
                }
            }
        }
        if (pageSize <= 0 || free < 0) {
            return unknown;
        }
        return new long[] {free * pageSize, (free + inactive + speculative) * pageSize};
    }
}
