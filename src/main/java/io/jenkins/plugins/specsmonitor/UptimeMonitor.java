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

/** Node monitor that reports how long the machine of each node has been running. */
public class UptimeMonitor extends NodeMonitor {

    @DataBoundConstructor
    public UptimeMonitor() {}

    @Extension
    @Symbol("uptimeMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<UptimeInfo> {

        @Override
        protected MasterToSlaveCallable<UptimeInfo, IOException> createCallable(Computer c) {
            return new GetUptimeInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.uptimeMonitor_displayName();
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

    /** Runs on the node itself, so it reports the node's uptime. */
    private static final class GetUptimeInfo extends MasterToSlaveCallable<UptimeInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(GetUptimeInfo.class.getName());

        /** Prints the seconds since the last boot. */
        private static final String WINDOWS_SCRIPT =
                "[int64]((Get-Date) - (Get-CimInstance Win32_OperatingSystem).LastBootUpTime).TotalSeconds";

        @Override
        public UptimeInfo call() {
            long seconds = detect();
            if (seconds < 0) {
                // Fallback: the uptime of the agent process instead of the machine.
                seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
            }
            return new UptimeInfo(seconds);
        }

        private static long detect() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.startsWith("windows")) {
                return UptimeParser.fromSeconds(CommandRunner.tryRun(
                        "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_SCRIPT));
            }
            if (os.startsWith("mac")) {
                return UptimeParser.fromMacBootTime(
                        CommandRunner.tryRun("sysctl", "-n", "kern.boottime"), System.currentTimeMillis() / 1000);
            }
            if (os.startsWith("linux")) {
                try {
                    return UptimeParser.fromSeconds(Files.readString(Path.of("/proc/uptime"), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Could not read /proc/uptime", e);
                }
            }
            return -1;
        }
    }
}
