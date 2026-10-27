package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The RAM of a node: its total size and, when the node can tell, the memory modules with their type and
 * speed. Reading the modules is best effort: VMs, containers and unprivileged users on Linux often get
 * no answer.
 */
public final class RamSpecs implements Serializable {
    private static final long serialVersionUID = 1L;

    /** No double quotes: Java mangles them when passing the command to PowerShell. */
    static final String WINDOWS_SCRIPT = "Get-CimInstance Win32_PhysicalMemory | ForEach-Object { "
            + "[string]$_.Capacity + '|' + [string]$_.ConfiguredClockSpeed + '|' + [string]$_.Speed + '|' "
            + "+ [string]$_.SMBIOSMemoryType }";

    private static final Pattern NUMBER = Pattern.compile("^\\s*(\\d+)\\s*([A-Za-z]*)");

    /** SMBIOS memory type codes. */
    private static final Map<Integer, String> SMBIOS_TYPES = Map.of(
            20, "DDR", 21, "DDR2", 24, "DDR3", 26, "DDR4", 27, "LPDDR", 28, "LPDDR2", 29, "LPDDR3", 30, "LPDDR4", 34,
            "DDR5", 35, "LPDDR5");

    private final long totalBytes;
    private final ArrayList<Module> modules;

    RamSpecs(long totalBytes, List<Module> modules) {
        this.totalBytes = totalBytes;
        this.modules = new ArrayList<>(modules);
    }

    /** Total physical memory in bytes, or a value {@code <= 0} if unknown. */
    long getTotalBytes() {
        return totalBytes;
    }

    public String getTotalDisplay() {
        return totalBytes > 0 ? MemoryInfo.format(totalBytes) : "N/A";
    }

    public List<Module> getModules() {
        return modules;
    }

    /** Runs on the node itself. */
    static RamSpecs collect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<Module> modules = List.of();
        if (os.startsWith("windows")) {
            modules = fromPowerShell(CommandRunner.tryRun(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_SCRIPT));
        } else if (os.startsWith("mac")) {
            modules = fromKeyValues(CommandRunner.tryRun("system_profiler", "SPMemoryDataType"));
        } else if (os.startsWith("linux")) {
            // Only answers when run as root.
            modules = fromKeyValues(CommandRunner.tryRun("dmidecode", "-t", "memory"));
        }
        long total = modules.stream().mapToLong(Module::getSizeBytes).sum();
        if (total <= 0) {
            total = totalFromJvm();
        }
        return new RamSpecs(total, modules);
    }

    private static long totalFromJvm() {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean) {
            return bean.getTotalMemorySize();
        }
        return -1;
    }

    /** Parses lines of {@code capacity|configured speed|speed|SMBIOS type}, one per module. */
    static List<Module> fromPowerShell(String output) {
        List<Module> modules = new ArrayList<>();
        if (output == null) {
            return modules;
        }
        for (String line : output.split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 4) {
                continue;
            }
            long size = parseLong(parts[0]);
            if (size <= 0) {
                continue;
            }
            int configured = (int) parseLong(parts[1]);
            int speed = configured > 0 ? configured : (int) parseLong(parts[2]);
            modules.add(new Module(size, SMBIOS_TYPES.get((int) parseLong(parts[3])), speed));
        }
        return modules;
    }

    /**
     * Parses the {@code Key: value} output of {@code dmidecode -t memory} and
     * {@code system_profiler SPMemoryDataType}. A module starts at its {@code Size} line, or at the
     * {@code Memory} line of Macs with soldered memory.
     */
    static List<Module> fromKeyValues(String output) {
        List<Module> modules = new ArrayList<>();
        if (output == null) {
            return modules;
        }
        long size = 0;
        String type = null;
        int speed = 0;
        int configured = 0;
        boolean open = false;
        for (String line : output.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (key.equals("Size") || (key.equals("Memory") && parseSize(value) > 0)) {
                if (open && size > 0) {
                    modules.add(new Module(size, type, configured > 0 ? configured : speed));
                }
                open = true;
                size = parseSize(value);
                type = null;
                speed = 0;
                configured = 0;
            } else if (!open) {
                continue;
            } else if (key.equals("Type")) {
                type = usable(value);
            } else if (key.equals("Speed")) {
                speed = (int) parseLong(value);
            } else if (key.equals("Configured Memory Speed") || key.equals("Configured Clock Speed")) {
                configured = (int) parseLong(value);
            }
        }
        if (open && size > 0) {
            modules.add(new Module(size, type, configured > 0 ? configured : speed));
        }
        return modules;
    }

    /** A size such as {@code 16 GB} or {@code 8192 MB} in bytes, or 0 for anything else. */
    private static long parseSize(String value) {
        Matcher m = NUMBER.matcher(value);
        if (!m.find()) {
            return 0;
        }
        long n = Long.parseLong(m.group(1));
        switch (m.group(2).toUpperCase(Locale.ROOT)) {
            case "KB":
                return n << 10;
            case "MB":
                return n << 20;
            case "GB":
                return n << 30;
            case "TB":
                return n << 40;
            default:
                return 0;
        }
    }

    private static long parseLong(String value) {
        Matcher m = NUMBER.matcher(value);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    /** Drops placeholders such as {@code Unknown} and {@code <OUT OF SPEC>}. */
    private static String usable(String value) {
        return value.isEmpty() || value.startsWith("<") || value.equalsIgnoreCase("Unknown") ? null : value;
    }

    /** One memory module (DIMM) or the soldered memory of a device. */
    public static final class Module implements Serializable {
        private static final long serialVersionUID = 1L;

        private final long sizeBytes;
        private final String type;
        private final int speedMhz;

        Module(long sizeBytes, String type, int speedMhz) {
            this.sizeBytes = sizeBytes;
            this.type = type;
            this.speedMhz = speedMhz;
        }

        long getSizeBytes() {
            return sizeBytes;
        }

        String getType() {
            return type;
        }

        int getSpeedMhz() {
            return speedMhz;
        }

        public String getSizeDisplay() {
            return MemoryInfo.format(sizeBytes);
        }

        public String getTypeDisplay() {
            return type == null ? "N/A" : type;
        }

        public String getSpeedDisplay() {
            return speedMhz > 0 ? speedMhz + " MHz" : "N/A";
        }
    }
}
