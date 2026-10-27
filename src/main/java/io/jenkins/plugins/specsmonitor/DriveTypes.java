package io.jenkins.plugins.specsmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds out whether a drive is an SSD or an HDD. This is best effort: virtual disks of VMs and
 * clouds, network shares and containers often hide the real hardware, and then there is no answer.
 */
final class DriveTypes {

    /** No double quotes: Java mangles them when passing the command to PowerShell. */
    static final String WINDOWS_SCRIPT =
            "$pd = @{}; Get-PhysicalDisk | ForEach-Object { $pd[[string]$_.DeviceId] = $_ }; "
                    + "Get-Partition | Where-Object { $_.DriveLetter } | ForEach-Object { $d = $pd[[string]$_.DiskNumber]; "
                    + "if ($d) { [string]$_.DriveLetter + '|' + [string]$d.MediaType + '|' + [string]$d.BusType } }";

    private static final Pattern SOLID_STATE = Pattern.compile("Solid State:\\s*(Yes|No)");

    private DriveTypes() {}

    /** The type of every Windows drive that has one, by upper case drive letter, such as {@code C}. */
    static Map<String, String> forWindows() {
        return fromPowerShell(
                CommandRunner.tryRun("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_SCRIPT));
    }

    /** Parses lines of {@code drive letter|MediaType|BusType}, such as {@code C|SSD|NVMe}. */
    static Map<String, String> fromPowerShell(String output) {
        Map<String, String> types = new HashMap<>();
        if (output == null) {
            return types;
        }
        for (String line : output.split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 2 || parts[0].isBlank()) {
                continue;
            }
            String type = label(parts[1], parts.length > 2 && parts[2].equalsIgnoreCase("NVMe"));
            if (type != null) {
                types.put(parts[0].trim().toUpperCase(Locale.ROOT), type);
            }
        }
        return types;
    }

    /**
     * The type of the disk behind a Linux block device such as {@code /dev/sda1}, or {@code null}
     * if it is not a block device or the kernel does not say.
     */
    @SuppressFBWarnings(
            value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "sysfs is at a fixed location on Linux")
    static String forLinux(String device) {
        if (device == null || !device.startsWith("/dev/")) {
            return null;
        }
        try {
            // Resolves /dev/mapper/name and similar links to the kernel name, such as dm-0.
            Path kernelName = Path.of(device).toRealPath().getFileName();
            if (kernelName == null) {
                return null;
            }
            Path sys = Path.of("/sys/class/block", kernelName.toString()).toRealPath();
            // A partition has no queue directory, its parent, the disk, does.
            for (Path p = sys; p != null && p.getNameCount() > 2; p = p.getParent()) {
                Path rotational = p.resolve("queue/rotational");
                if (Files.isReadable(rotational)) {
                    String content = new String(Files.readAllBytes(rotational), StandardCharsets.UTF_8);
                    Path disk = p.getFileName();
                    return fromRotational(
                            content, disk != null && disk.toString().startsWith("nvme"));
                }
            }
        } catch (IOException | RuntimeException e) {
            // Not a device the kernel knows, such as inside a container.
        }
        return null;
    }

    /** Interprets {@code queue/rotational}: {@code 1} is a spinning disk, {@code 0} is not. */
    static String fromRotational(String content, boolean nvme) {
        if (content == null) {
            return null;
        }
        switch (content.trim()) {
            case "1":
                return "HDD";
            case "0":
                return nvme ? "SSD (NVMe)" : "SSD";
            default:
                return null;
        }
    }

    /** The type of the disk behind a macOS volume, from {@code diskutil info}. */
    static String forMac(String mount) {
        return fromDiskutil(CommandRunner.tryRun("diskutil", "info", mount));
    }

    static String fromDiskutil(String output) {
        if (output == null) {
            return null;
        }
        Matcher m = SOLID_STATE.matcher(output);
        if (!m.find()) {
            return null;
        }
        return m.group(1).equals("Yes") ? "SSD" : "HDD";
    }

    private static String label(String mediaType, boolean nvme) {
        switch (mediaType.trim().toUpperCase(Locale.ROOT)) {
            case "SSD":
                return nvme ? "SSD (NVMe)" : "SSD";
            case "HDD":
                return "HDD";
            default:
                // "Unspecified" or "SCM", as reported for virtual disks.
                return null;
        }
    }
}
