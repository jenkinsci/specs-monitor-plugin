package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A version of something on a node, as a short display value plus the full version for a tooltip. */
public class VersionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The numeric part of a version followed by a {@code .v<hash>} suffix, as used by remoting. */
    private static final Pattern HASH_SUFFIX = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\.v.*$");

    private final String display;
    private final String full;

    /**
     * @param display short version shown in the column
     * @param full complete version shown as a tooltip
     */
    public VersionInfo(String display, String full) {
        this.display = display;
        this.full = full;
    }

    /** Short version, e.g. {@code 25.0.4.1}, or {@code N/A} if unknown. */
    public String getDisplay() {
        return display == null || display.isBlank() ? "N/A" : display;
    }

    /** Complete version, e.g. {@code 25.0.4.1+1-LTS}. Empty if unknown. */
    public String getTooltip() {
        return full == null ? "" : full;
    }

    /** The JVM version {@code 25.0.4.1+1-LTS} as {@code 25.0.4.1} with the full version as tooltip. */
    static VersionInfo fromJvm(Runtime.Version version) {
        StringBuilder numbers = new StringBuilder();
        for (int component : version.version()) {
            if (numbers.length() > 0) {
                numbers.append('.');
            }
            numbers.append(component);
        }
        return new VersionInfo(numbers.toString(), version.toString());
    }

    /**
     * A remoting version {@code 3355.3357.v931d3c992987} as {@code 3355.3357} with the full version as
     * tooltip. A version without a {@code .v<hash>} suffix is shown as is.
     */
    static VersionInfo fromRemoting(String version) {
        if (version == null) {
            return new VersionInfo(null, null);
        }
        Matcher matcher = HASH_SUFFIX.matcher(version);
        return new VersionInfo(matcher.matches() ? matcher.group(1) : version, version);
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
