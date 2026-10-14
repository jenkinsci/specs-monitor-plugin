package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GPU model names detected on a node. */
public class GpuInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Pattern LINUX_CONTROLLER = Pattern.compile(
            "(?i)\\b(?:VGA compatible controller|3D controller|Display controller):\\s*(.+?)(?:\\s+\\(rev [^)]+\\))?$");
    private static final Pattern MAC_CHIPSET = Pattern.compile("^\\s*Chipset Model: (.+?)\\s*$");
    private static final Pattern WINDOWS_ADAPTER = Pattern.compile("^(.+?)\\t(.*)$");

    private final String name;

    public GpuInfo(String name) {
        this.name = name == null || name.isBlank() ? "N/A" : name;
    }

    private GpuInfo(List<String> names) {
        this(names.stream()
                .map(name -> name == null ? "" : name.trim())
                .filter(name -> !name.isEmpty())
                .findFirst()
                .orElse("N/A"));
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return getName();
    }

    public String getTooltip() {
        return getName();
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
        List<String> discrete = new ArrayList<>();
        List<String> pci = new ArrayList<>();
        List<String> other = new ArrayList<>();
        List<String> all = new ArrayList<>();
        if (output != null) {
            for (String line : output.lines().toList()) {
                Matcher matcher = WINDOWS_ADAPTER.matcher(line.trim());
                String name = matcher.matches() ? matcher.group(1).trim() : line.trim();
                if (name.isEmpty()) {
                    continue;
                }
                all.add(name);
                if (!matcher.matches()) {
                    if (!isVirtualAdapter(name)) {
                        other.add(name);
                    }
                    continue;
                }

                String deviceId = matcher.group(2).trim().toUpperCase(Locale.ROOT);
                if (deviceId.matches("PCI\\\\VEN_(10DE|1002).*")) {
                    discrete.add(name);
                } else if (deviceId.startsWith("PCI\\\\")) {
                    pci.add(name);
                } else if (!isVirtualAdapter(name)) {
                    other.add(name);
                }
            }
        }
        if (!discrete.isEmpty()) {
            return fromLines(discrete);
        }
        if (!pci.isEmpty()) {
            return fromLines(pci);
        }
        return fromLines(other.isEmpty() ? all : other);
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
        return new GpuInfo(lines);
    }

    private static boolean isVirtualAdapter(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return lowerName.contains("virtual")
                || lowerName.contains("remote display")
                || lowerName.contains("indirect display")
                || lowerName.contains("basic display")
                || lowerName.contains("parsec")
                || lowerName.contains("spacedesk");
    }
}
