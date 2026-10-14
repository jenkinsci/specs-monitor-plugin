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
    private static final Pattern NVIDIA_QUADRO_MODEL =
            Pattern.compile("\\bQuadro\\s+(?:RTX\\s+A?\\d{3,4}|P\\d{4})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NVIDIA_MODEL = Pattern.compile(
            "\\b(?:RTX|GTX)\\s+A?\\d{3,4}M?(?:\\s+(?:Ti|SUPER))?(?:\\s+Laptop)?(?:\\s+(?:Max-Q|with\\s+Max-Q\\s+Design))?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NVIDIA_ACCELERATOR =
            Pattern.compile("\\bTesla\\s+P?V?\\d{2,3}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMD_MODEL = Pattern.compile(
            "\\b(?:RX\\s*(?:\\d{4}(?:M|S)?|Vega\\s+M\\s+GL)(?:\\s+(?:XT|XTX|GRE|S))?|AI\\s+PRO\\s+R\\d+|Pro\\s+WX\\s+\\d+|Instinct\\s+MI\\d+[A-Z]?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INTEL_ARC_MODEL =
            Pattern.compile("\\bArc\\s+(?:Pro\\s+)?[A-Z]?\\d{1,3}[A-Z]?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTEL_DATA_CENTER_MODEL =
            Pattern.compile("\\b(?:Max|Flex)\\s+\\d{2,4}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRADEMARK = Pattern.compile("\\((?:R|TM)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VENDOR_PREFIX = Pattern.compile(
            "(?i)^\\s*(?:(?:NVIDIA|Intel|AMD)(?:\\s+Corporation)?|Advanced Micro Devices(?:, Inc\\.)?)(?:\\s+\\[AMD/ATI\\])?\\s*");

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
        return getShortName();
    }

    public String getShortName() {
        String cleanedName = TRADEMARK.matcher(name).replaceAll("");
        String model = findModel(NVIDIA_QUADRO_MODEL, cleanedName);
        if (model == null) {
            model = findModel(NVIDIA_MODEL, cleanedName);
        }
        if (model == null) {
            model = findModel(NVIDIA_ACCELERATOR, cleanedName);
        }
        if (model == null) {
            model = findModel(AMD_MODEL, cleanedName);
        }
        if (model == null) {
            model = findModel(INTEL_ARC_MODEL, cleanedName);
        }
        if (model == null) {
            model = findModel(INTEL_DATA_CENTER_MODEL, cleanedName);
        }
        if (model != null) {
            return normalizeModel(model.replaceFirst("(?i)\\s+with\\s+Max-Q\\s+Design$", " Max-Q"));
        }
        cleanedName = VENDOR_PREFIX.matcher(cleanedName).replaceFirst("");
        return cleanedName.replaceAll("\\s+", " ").trim();
    }

    public String getTooltip() {
        return getName();
    }

    private static String findModel(Pattern pattern, String name) {
        Matcher matcher = pattern.matcher(name);
        return matcher.find() ? matcher.group() : null;
    }

    private static String normalizeModel(String model) {
        int separator = model.indexOf(' ');
        String family = separator < 0 ? model : model.substring(0, separator);
        String normalizedFamily =
                switch (family.toLowerCase(Locale.ROOT)) {
                    case "arc" -> "Arc";
                    case "max" -> "Max";
                    case "flex" -> "Flex";
                    case "quadro" -> "Quadro";
                    case "tesla" -> "Tesla";
                    case "instinct" -> "Instinct";
                    case "pro" -> "Pro";
                    default -> family.toUpperCase(Locale.ROOT);
                };
        String normalized = separator < 0 ? normalizedFamily : normalizedFamily + model.substring(separator);
        return normalized.replaceAll("(?i)\\bSUPER\\b", "SUPER");
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
