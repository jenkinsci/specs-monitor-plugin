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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/** Node monitor that reports the total physical memory (RAM) of each node. */
public class MemoryMonitor extends NodeMonitor {

    @DataBoundConstructor
    public MemoryMonitor() {}

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
    }

    /** Runs on the node itself, so it reports the node's memory. */
    private static final class GetMemoryInfo extends MasterToSlaveCallable<MemoryInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(GetMemoryInfo.class.getName());

        @Override
        public MemoryInfo call() throws IOException {
            long bytes = detectTotalBytes();
            if (bytes <= 0) {
                bytes = totalFromJvm();
            }
            return new MemoryInfo(bytes);
        }

        private static long detectTotalBytes() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.startsWith("windows")) {
                return MemoryParser.fromBytes(CommandRunner.tryRun(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-Command",
                        "(Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory"));
            }
            if (os.startsWith("mac")) {
                return MemoryParser.fromBytes(CommandRunner.tryRun("sysctl", "-n", "hw.memsize"));
            }
            if (os.startsWith("linux")) {
                try {
                    return MemoryParser.fromMeminfo(
                            Files.readAllLines(Path.of("/proc/meminfo"), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Could not read /proc/meminfo", e);
                }
            }
            return -1;
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
    }
}
