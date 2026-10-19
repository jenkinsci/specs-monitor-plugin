package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Launcher;
import java.io.IOException;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Node monitor that reports the remoting version of each node, with the full version string as a
 * tooltip.
 */
public class RemotingVersionMonitor extends NodeMonitor {

    @DataBoundConstructor
    public RemotingVersionMonitor() {}

    @Extension
    @Symbol("remotingVersionMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<VersionInfo> {

        @Override
        protected MasterToSlaveCallable<VersionInfo, IOException> createCallable(Computer c) {
            return new GetRemotingVersion();
        }

        @Override
        public String getDisplayName() {
            return Messages.remotingVersionMonitor_displayName();
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

    /** Runs on the node itself, so it reports the remoting version loaded by the node. */
    private static final class GetRemotingVersion extends MasterToSlaveCallable<VersionInfo, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public VersionInfo call() {
            return VersionInfo.fromRemoting(Launcher.VERSION);
        }
    }
}
