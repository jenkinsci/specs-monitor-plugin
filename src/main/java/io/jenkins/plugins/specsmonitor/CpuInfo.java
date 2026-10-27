package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * CPU model name and number of hardware threads (logical processors) of a node.
 */
@ExportedBean
public class CpuInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Trademark marks are removed without a replacement, so "FX(tm)-8320" becomes
     * "FX-8320".
     */
    private static final Pattern TRADEMARK = Pattern.compile("\\((?:R|TM)\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern NOISE = Pattern.compile(
            "@\\s*[\\d.]+\\s*[GM]Hz" // @ 2.40GHz
                    + "|\\b\\d+(?:st|nd|rd|th)\\s+Gen\\b" // 13th Gen
                    + "|\\b(?:\\d+|Single|Dual|Triple|Quad|Penta|Hexa|Six|Octa|Eight|Ten|Twelve|Sixteen)-Cores?\\b" // 8-Core,
                    // Eight-Core
                    + "|\\b(?:Intel|AMD|Core|CPU|Processor)\\b", // vendor/filler words
            Pattern.CASE_INSENSITIVE);

    /** Matches a clock speed that is already part of the CPU name, such as "3.20GHz". */
    private static final Pattern CLOCK = Pattern.compile("\\d\\s*[GM]Hz", Pattern.CASE_INSENSITIVE);

    private final String name;
    private final int threads;

    /** Clock speed in MHz, or 0 if unknown. Also 0 when sent by an older plugin version. */
    private final int maxMhz;

    public CpuInfo(String name, int threads) {
        this(name, threads, 0);
    }

    public CpuInfo(String name, int threads, int maxMhz) {
        this.name = name;
        this.threads = threads;
        this.maxMhz = maxMhz;
    }

    @Exported
    public String getName() {
        return name;
    }

    /**
     * Clock speed in MHz, or 0 if unknown. This is the maximum frequency where the node reports
     * one, otherwise the nominal or current frequency.
     */
    @Exported
    public int getMaxMhz() {
        return maxMhz;
    }

    /**
     * The full name with the clock speed appended in the style of Intel names
     * ("@ 3.20GHz"), unless the name already contains one or the speed is unknown.
     */
    public String getNameWithClock() {
        if (name == null || maxMhz <= 0 || CLOCK.matcher(name).find()) {
            return name;
        }
        return String.format(Locale.ROOT, "%s @ %.2fGHz", name, maxMhz / 1000.0);
    }

    @Exported
    public String getShortName() {
        String withoutMarks = TRADEMARK.matcher(name == null ? "" : name).replaceAll("");
        String s = NOISE.matcher(withoutMarks)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
        return s.isEmpty() ? name : s;
    }

    /**
     * Number of logical processors (hardware threads) available to the JVM on the
     * node.
     */
    @Exported
    public int getThreads() {
        return threads;
    }

    @Exported
    public String getScore() {
        return CpuScoreLookup.formatFor(getShortName());
    }

    @Override
    public String toString() {
        return getShortName() + " (" + threads + " threads)";
    }
}
