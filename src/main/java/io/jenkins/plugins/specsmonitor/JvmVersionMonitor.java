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
 * Node monitor that reports the version of the JVM running on each node, with the full version
 * string as a tooltip.
 */
public class JvmVersionMonitor extends NodeMonitor {

    @DataBoundConstructor
    public JvmVersionMonitor() {}

    @Extension
    @Symbol("jvmVersionMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<VersionInfo> {

        @Override
        protected MasterToSlaveCallable<VersionInfo, IOException> createCallable(Computer c) {
            return new GetJvmVersion();
        }

        @Override
        public String getDisplayName() {
            return Messages.jvmVersionMonitor_displayName();
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

    /** Runs on the node itself, so it reports the version of the node's JVM. */
    private static final class GetJvmVersion extends MasterToSlaveCallable<VersionInfo, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public VersionInfo call() {
            return VersionInfo.fromJvm(Runtime.version());
        }
    }
}
