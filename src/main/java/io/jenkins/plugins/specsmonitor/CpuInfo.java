package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * CPU model name and number of hardware threads (logical processors) of a node.
 */
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
                    + "|\\b(?:\\d+|Single|Dual|Triple|Quad|Penta|Hexa|Six|Octa|Eight|Ten|Twelve|Sixteen)-Core\\b" // 8-Core,
                    // Eight-Core
                    + "|\\b(?:Intel|AMD|Core|CPU|Processor)\\b", // vendor/filler words
            Pattern.CASE_INSENSITIVE);

    private final String name;
    private final int threads;

    public CpuInfo(String name, int threads) {
        this.name = name;
        this.threads = threads;
    }

    public String getName() {
        return name;
    }

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
    public int getThreads() {
        return threads;
    }

    @Override
    public String toString() {
        return getShortName() + " (" + threads + " threads)";
    }
}
