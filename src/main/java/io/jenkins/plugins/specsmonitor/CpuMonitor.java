package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
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

/** Node monitor that reports the CPU model and thread count of each node. */
public class CpuMonitor extends NodeMonitor {

    @DataBoundConstructor
    public CpuMonitor() {}

    @Extension
    @Symbol("cpuMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<CpuInfo> {

        @Override
        protected MasterToSlaveCallable<CpuInfo, IOException> createCallable(Computer c) {
            return new GetCpuInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.cpuMonitor_displayName();
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

    /**
     * Runs on the node itself, so it reports the node's hardware. Supports Windows,
     * Linux and macOS;
     * other systems report N/A.
     */
    static final class GetCpuInfo extends MasterToSlaveCallable<CpuInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(GetCpuInfo.class.getName());

        /** CPU name and clock speed in MHz as read from the node; empty and 0 if unknown. */
        private record Detected(String name, int mhz) {}

        @Override
        public CpuInfo call() throws IOException {
            int threads = Runtime.getRuntime().availableProcessors();
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            Detected detected = new Detected("", 0);
            if (os.startsWith("windows")) {
                detected = detectWindows();
            } else if (os.startsWith("mac")) {
                detected = detectMac();
            } else if (os.startsWith("linux")) {
                detected = detectLinux();
            }
            return new CpuInfo(cleanName(detected.name()), threads, detected.mhz());
        }

        private static String cleanName(String name) {
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

        private static Detected detectWindows() {
            // Prints the name on the first line and the maximum clock speed in MHz on the
            // second.
            String out = CommandRunner.tryRun(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    "$p = Get-CimInstance Win32_Processor | Select-Object -First 1; $p.Name; $p.MaxClockSpeed");
            String[] lines = out.split("\\R");
            if (!lines[0].isBlank()) {
                return new Detected(lines[0], lines.length > 1 ? parseInt(lines[1].trim(), 10) : 0);
            }
            String key = "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0";
            out = CommandRunner.tryRun("reg", "query", key, "/v", "ProcessorNameString");
            int i = out.indexOf("REG_SZ");
            String name = i >= 0 ? out.substring(i + "REG_SZ".length()).trim() : "";
            // ~MHz holds the nominal clock speed as a hex DWORD, e.g. "0xc80".
            out = CommandRunner.tryRun("reg", "query", key, "/v", "~MHz");
            i = out.indexOf("0x");
            int mhz = i >= 0 ? parseInt(out.substring(i + 2).trim(), 16) : 0;
            return new Detected(name, mhz);
        }

        private static Detected detectMac() {
            String name = CommandRunner.tryRun("sysctl", "-n", "machdep.cpu.brand_string");
            // Only Intel Macs report a frequency (in Hz); Apple Silicon does not.
            long hz = parseLong(CommandRunner.tryRun("sysctl", "-n", "hw.cpufrequency_max"));
            return new Detected(name, (int) (hz / 1_000_000));
        }

        private static Detected detectLinux() {
            // lscpu knows how to name CPUs that /proc/cpuinfo only describes with numeric
            // IDs (e.g. aarch64).
            String lscpu = CommandRunner.tryRun("lscpu");
            String name = CpuNameParser.fromLscpu(lscpu);
            int mhz = CpuNameParser.maxMhzFromLscpu(lscpu);
            if (!name.isEmpty() && mhz > 0) {
                return new Detected(name, mhz);
            }
            try {
                List<String> cpuinfo = Files.readAllLines(Path.of("/proc/cpuinfo"), StandardCharsets.UTF_8);
                return new Detected(
                        name.isEmpty() ? CpuNameParser.fromProcCpuinfo(cpuinfo) : name,
                        mhz > 0 ? mhz : CpuNameParser.mhzFromProcCpuinfo(cpuinfo));
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Could not read /proc/cpuinfo", e);
                return new Detected(name, mhz);
            }
        }

        private static int parseInt(String s, int radix) {
            try {
                return Integer.parseInt(s, radix);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static long parseLong(String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
