package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/** Node monitor that reports the CPU model and thread count of each node. */
public class SpecsMonitor extends NodeMonitor {

    @DataBoundConstructor
    public SpecsMonitor() {}

    @Extension
    @Symbol("specsMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<CpuInfo> {

        @Override
        protected MasterToSlaveCallable<CpuInfo, IOException> createCallable(Computer c) {
            return new GetCpuInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.displayName();
        }
    }

    /**
     * Runs on the node itself, so it reports the node's hardware. Supports Windows,
     * Linux and macOS;
     * other systems report N/A.
     */
    private static final class GetCpuInfo extends MasterToSlaveCallable<CpuInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(GetCpuInfo.class.getName());

        @Override
        public CpuInfo call() throws IOException {
            int threads = Runtime.getRuntime().availableProcessors();
            return new CpuInfo(detectName(), threads);
        }

        private static String detectName() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String name = "";
            if (os.startsWith("windows")) {
                name = detectWindows();
            } else if (os.startsWith("mac")) {
                name = CommandRunner.tryRun("sysctl", "-n", "machdep.cpu.brand_string");
            } else if (os.startsWith("linux")) {
                name = detectLinux();
            }
            if (!name.isBlank()) {
                return name.replaceAll("\\s+", " ").trim();
            }
            // Other systems (AIX, Solaris, BSD, ...) and detection failures. The
            // architecture is already
            // shown by the built-in architecture monitor, so there is nothing more useful
            // to report.
            String identifier = System.getenv("PROCESSOR_IDENTIFIER");
            if (identifier != null && !identifier.isBlank()) {
                return identifier;
            }
            return "N/A";
        }

        private static String detectWindows() {
            String out = CommandRunner.tryRun(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    "(Get-CimInstance Win32_Processor | Select-Object -First 1).Name");
            if (!out.isBlank()) {
                return out;
            }
            out = CommandRunner.tryRun(
                    "reg",
                    "query",
                    "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
                    "/v",
                    "ProcessorNameString");
            int i = out.indexOf("REG_SZ");
            return i >= 0 ? out.substring(i + "REG_SZ".length()).trim() : "";
        }

        private static String detectLinux() {
            // lscpu knows how to name CPUs that /proc/cpuinfo only describes with numeric
            // IDs (e.g. aarch64).
            String name = CpuNameParser.fromLscpu(CommandRunner.tryRun("lscpu"));
            if (!name.isEmpty()) {
                return name;
            }
            try {
                return CpuNameParser.fromProcCpuinfo(
                        Files.readAllLines(Path.of("/proc/cpuinfo"), StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Could not read /proc/cpuinfo", e);
                return "";
            }
        }
    }
}
