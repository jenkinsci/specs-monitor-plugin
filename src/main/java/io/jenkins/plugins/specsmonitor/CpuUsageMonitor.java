package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.management.ManagementFactory;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Node monitor that reports the CPU usage of each node, with the usage of the agent process as a
 * tooltip.
 */
public class CpuUsageMonitor extends NodeMonitor {

    /** Default for {@link #getWarnAbovePercent()}. */
    public static final int DEFAULT_WARN_ABOVE_PERCENT = 99;

    private int warnAbovePercent = DEFAULT_WARN_ABOVE_PERCENT;

    @DataBoundConstructor
    public CpuUsageMonitor() {}

    /** CPU usage at or above this percentage is highlighted as a warning. */
    public int getWarnAbovePercent() {
        return warnAbovePercent;
    }

    @DataBoundSetter
    public void setWarnAbovePercent(int warnAbovePercent) {
        this.warnAbovePercent = Math.max(0, Math.min(100, warnAbovePercent));
    }

    /**
     * Whether the system CPU usage in {@code info} has reached the configured threshold. A
     * threshold of 100 turns the warning off.
     */
    public boolean isHigh(CpuUsageInfo info) {
        if (info == null || info.getSystemPercent() < 0 || warnAbovePercent >= 100) {
            return false;
        }
        return info.getSystemPercent() >= warnAbovePercent;
    }

    @Extension
    @Symbol("cpuUsageMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<CpuUsageInfo> {

        @Override
        protected MasterToSlaveCallable<CpuUsageInfo, IOException> createCallable(Computer c) {
            return new GetCpuUsageInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.cpuUsageMonitor_displayName();
        }

        public int getDefaultWarnAbovePercent() {
            return DEFAULT_WARN_ABOVE_PERCENT;
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

    /** Runs on the node itself, so it reports the node's CPU usage. */
    private static final class GetCpuUsageInfo extends MasterToSlaveCallable<CpuUsageInfo, IOException> {
        private static final long serialVersionUID = 1L;

        /** How long CPU usage is measured. Short enough to stay well within the monitor timeout. */
        private static final long SAMPLE_MILLIS = 1000;

        @Override
        public CpuUsageInfo call() throws IOException {
            if (!(ManagementFactory.getOperatingSystemMXBean()
                    instanceof com.sun.management.OperatingSystemMXBean bean)) {
                return new CpuUsageInfo(-1, -1);
            }
            // Both values are measured since the previous call, which can be long ago or never, so
            // the first call starts a fresh sample.
            bean.getCpuLoad();
            bean.getProcessCpuLoad();
            try {
                Thread.sleep(SAMPLE_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted while measuring CPU usage");
            }
            return new CpuUsageInfo(toPercent(bean.getCpuLoad()), toPercent(bean.getProcessCpuLoad()));
        }

        /** Converts a load between 0 and 1 to percent; negative or NaN (unavailable) becomes -1. */
        private static double toPercent(double load) {
            return load >= 0 ? Math.min(100, load * 100) : -1;
        }
    }
}
