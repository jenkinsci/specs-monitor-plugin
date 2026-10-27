package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.util.Locale;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/** Node monitor that reports GPU model names for each node. */
public class GpuMonitor extends NodeMonitor {

    @DataBoundConstructor
    public GpuMonitor() {}

    @Extension
    @Symbol("gpuMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<GpuInfo> {

        @Override
        protected MasterToSlaveCallable<GpuInfo, IOException> createCallable(Computer c) {
            return new GetGpuInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.gpuMonitor_displayName();
        }

        @Override
        public boolean canTakeOffline() {
            return false;
        }
    }

    static final class GetGpuInfo extends MasterToSlaveCallable<GpuInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final String WINDOWS_SCRIPT = "Get-CimInstance Win32_VideoController | "
                + "Where-Object { $_.Name } | ForEach-Object { $_.Name + [char]9 + $_.PNPDeviceID }";

        @Override
        public GpuInfo call() throws IOException {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.startsWith("windows")) {
                GpuInfo info = GpuInfo.fromWindows(CommandRunner.tryRun(
                        "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_SCRIPT));
                return info.getName().equals("N/A") ? nvidiaFallback() : info;
            }
            if (os.startsWith("linux")) {
                GpuInfo info = GpuInfo.fromLinuxLspci(CommandRunner.tryRun("lspci"));
                return info.getName().equals("N/A") ? nvidiaFallback() : info;
            }
            if (os.startsWith("mac")) {
                GpuInfo info =
                        GpuInfo.fromMacSystemProfiler(CommandRunner.tryRun("system_profiler", "SPDisplaysDataType"));
                return info.getName().equals("N/A") ? nvidiaFallback() : info;
            }
            return nvidiaFallback();
        }

        private static GpuInfo nvidiaFallback() {
            return GpuInfo.fromNvidiaSmi(
                    CommandRunner.tryRun("nvidia-smi", "--query-gpu=name", "--format=csv,noheader"));
        }
    }
}
