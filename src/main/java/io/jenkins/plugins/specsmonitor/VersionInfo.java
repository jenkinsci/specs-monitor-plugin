package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A version of something on a node, as a short display value plus the full version for a tooltip. */
public class VersionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The numeric part of a version followed by a {@code .v<hash>} suffix, as used by remoting. */
    private static final Pattern HASH_SUFFIX = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\.v.*$");

    /** The output of {@code git --version}: group 1 is the full version, group 2 the numeric version. */
    private static final Pattern GIT_VERSION = Pattern.compile("git version ((\\d+(?:\\.\\d+)*)[^\\r\\n]*)");

    private static final Pattern NUMBERS = Pattern.compile("\\d+(?:\\.\\d+)*");

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

    /**
     * The output of {@code git --version}, e.g. {@code git version 2.43.0.windows.1}, as
     * {@code 2.43.0} with {@code 2.43.0.windows.1} as tooltip. Unknown if the output is not
     * recognized, for example because Git is not installed.
     */
    static VersionInfo fromGit(String output) {
        if (output == null) {
            return new VersionInfo(null, null);
        }
        Matcher matcher = GIT_VERSION.matcher(output.trim());
        if (!matcher.lookingAt()) {
            return new VersionInfo(null, null);
        }
        return new VersionInfo(matcher.group(2), matcher.group(1).trim());
    }

    /** Whether the first number (the JVM feature release) is lower than {@code other}'s. */
    boolean isFeatureOlderThan(VersionInfo other) {
        int[] mine = numbers(getDisplay());
        int[] theirs = numbers(other.getDisplay());
        return mine.length > 0 && theirs.length > 0 && mine[0] < theirs[0];
    }

    /**
     * Whether the numbers of this version are lower than those of {@code other}, compared one
     * component at a time. A missing component counts as 0. Unknown versions are never older.
     */
    boolean isOlderThan(VersionInfo other) {
        int[] mine = numbers(getDisplay());
        int[] theirs = numbers(other.getDisplay());
        if (mine.length == 0 || theirs.length == 0) {
            return false;
        }
        for (int i = 0; i < Math.max(mine.length, theirs.length); i++) {
            int a = i < mine.length ? mine[i] : 0;
            int b = i < theirs.length ? theirs[i] : 0;
            if (a != b) {
                return a < b;
            }
        }
        return false;
    }

    /** The leading dot-separated numbers of {@code version}, or an empty array if there are none. */
    private static int[] numbers(String version) {
        Matcher matcher = NUMBERS.matcher(version);
        if (!matcher.lookingAt()) {
            return new int[0];
        }
        String[] parts = matcher.group().split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return new int[0];
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
