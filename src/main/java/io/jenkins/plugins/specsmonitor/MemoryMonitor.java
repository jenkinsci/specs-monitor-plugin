package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Node monitor that reports the total physical memory (RAM) of each node, with
 * free memory as a tooltip.
 */
public class MemoryMonitor extends NodeMonitor {

    /** Default for {@link #getWarnBelowPercent()}. */
    public static final int DEFAULT_WARN_BELOW_PERCENT = 10;

    private int warnBelowPercent = DEFAULT_WARN_BELOW_PERCENT;

    @DataBoundConstructor
    public MemoryMonitor() {}

    /** Free memory below this percentage of the total is highlighted as a warning. */
    public int getWarnBelowPercent() {
        return warnBelowPercent;
    }

    @DataBoundSetter
    public void setWarnBelowPercent(int warnBelowPercent) {
        this.warnBelowPercent = Math.max(0, Math.min(100, warnBelowPercent));
    }

    /** Whether the usable memory in {@code info} is below the configured threshold. */
    public boolean isLow(MemoryInfo info) {
        if (info == null || info.getTotalBytes() <= 0 || info.getUsableBytes() < 0) {
            return false;
        }
        return info.getUsableBytes() * 100.0 / info.getTotalBytes() < warnBelowPercent;
    }

    @Extension
    @Symbol("memoryMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<MemoryInfo> {

        @Override
        protected MasterToSlaveCallable<MemoryInfo, IOException> createCallable(Computer c) {
            return new GetMemoryInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.memoryMonitor_displayName();
        }

        public int getDefaultWarnBelowPercent() {
            return DEFAULT_WARN_BELOW_PERCENT;
        }

        /**
         * This monitor only reports information, so there is no threshold to take a
         * node offline.
         */
        @Override
        public boolean canTakeOffline() {
            return false;
        }
    }

    /** Runs on the node itself, so it reports the node's memory. */
    private static final class GetMemoryInfo extends MasterToSlaveCallable<MemoryInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(GetMemoryInfo.class.getName());

        /** Prints "total free available" in bytes on one line. */
        private static final String WINDOWS_SCRIPT = "$os = Get-CimInstance Win32_OperatingSystem; "
                + "$perf = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory; "
                + "@((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory, "
                + "($os.FreePhysicalMemory * 1024), $perf.AvailableBytes) -join ' '";

        @Override
        public MemoryInfo call() throws IOException {
            // {total, free, available} in bytes, -1 if unknown
            long[] values = detect();
            long total = values[0];
            long free = values[1];
            long available = values[2];
            if (total <= 0) {
                total = totalFromJvm();
                if (free < 0) {
                    free = freeFromJvm();
                }
            }
            return new MemoryInfo(total, free, available);
        }

        private static long[] detect() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.startsWith("windows")) {
                return MemoryParser.fromBytesList(
                        CommandRunner.tryRun(
                                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_SCRIPT),
                        3);
            }
            if (os.startsWith("mac")) {
                long total = MemoryParser.fromBytes(CommandRunner.tryRun("sysctl", "-n", "hw.memsize"));
                long[] vm = MemoryParser.fromVmStat(CommandRunner.tryRun("vm_stat"));
                return new long[] {total, vm[0], vm[1]};
            }
            if (os.startsWith("linux")) {
                try {
                    List<String> lines = Files.readAllLines(Path.of("/proc/meminfo"), StandardCharsets.UTF_8);
                    return new long[] {
                        MemoryParser.fromMeminfo(lines),
                        MemoryParser.fromMeminfoKey(lines, "MemFree"),
                        MemoryParser.fromMeminfoKey(lines, "MemAvailable")
                    };
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Could not read /proc/meminfo", e);
                }
            }
            return new long[] {-1, -1, -1};
        }

        /**
         * Fallback for other systems and failed detection. May reflect a container
         * limit.
         */
        private static long totalFromJvm() {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean) {
                return bean.getTotalMemorySize();
            }
            return -1;
        }

        private static long freeFromJvm() {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean) {
                return bean.getFreeMemorySize();
            }
            return -1;
        }
    }
}
