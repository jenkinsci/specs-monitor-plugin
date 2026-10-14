package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GPU model names detected on a node. */
public class GpuInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Pattern LINUX_CONTROLLER = Pattern.compile(
            "(?i)\\b(?:VGA compatible controller|3D controller|Display controller):\\s*(.+?)(?:\\s+\\(rev [^)]+\\))?$");
    private static final Pattern MAC_CHIPSET = Pattern.compile("^\\s*Chipset Model: (.+?)\\s*$");

    private final String name;

    public GpuInfo(String name) {
        this.name = name == null || name.isBlank() ? "N/A" : name;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return name;
    }

    public String getTooltip() {
        return name;
    }

    static GpuInfo fromNvidiaSmi(String output) {
        List<String> names = output == null
                ? List.of()
                : output.lines()
                        .filter(line -> !line.startsWith("NVIDIA-SMI")
                                && !line.startsWith("Failed to")
                                && !line.equalsIgnoreCase("No devices were found"))
                        .toList();
        return fromLines(names);
    }

    static GpuInfo fromWindows(String output) {
        return fromLines(output == null ? List.of() : output.lines().toList());
    }

    static GpuInfo fromLinuxLspci(String output) {
        List<String> names = new ArrayList<>();
        if (output != null) {
            for (String line : output.lines().toList()) {
                Matcher matcher = LINUX_CONTROLLER.matcher(line);
                if (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return fromLines(names);
    }

    static GpuInfo fromMacSystemProfiler(String output) {
        List<String> names = new ArrayList<>();
        if (output != null) {
            for (String line : output.lines().toList()) {
                Matcher matcher = MAC_CHIPSET.matcher(line);
                if (matcher.matches()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return fromLines(names);
    }

    private static GpuInfo fromLines(List<String> lines) {
        List<String> names = lines.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
        return new GpuInfo(String.join(", ", names));
    }
}
