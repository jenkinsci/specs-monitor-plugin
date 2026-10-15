package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Node monitor that reports the free memory of the JVM running on each node,
 * with free out of the maximum heap size as a tooltip.
 */
public class JvmMemoryMonitor extends NodeMonitor {

    @DataBoundConstructor
    public JvmMemoryMonitor() {}

    @Extension
    @Symbol("jvmMemoryMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<MemoryInfo> {

        @Override
        protected MasterToSlaveCallable<MemoryInfo, IOException> createCallable(Computer c) {
            return new GetJvmMemoryInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.jvmMemoryMonitor_displayName();
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

    /** Runs on the node itself, so it reports the memory of the node's JVM. */
    private static final class GetJvmMemoryInfo extends MasterToSlaveCallable<MemoryInfo, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public MemoryInfo call() {
            Runtime runtime = Runtime.getRuntime();
            long max = runtime.maxMemory();
            long used = runtime.totalMemory() - runtime.freeMemory();
            long free = Math.max(0, max - used);
            return new MemoryInfo(max, free, free);
        }
    }
}
