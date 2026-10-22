package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Node monitor that reports how each node is connected to the controller, such
 * as WebSocket, JNLP (inbound TCP) or SSH.
 */
public class ConnectionMonitor extends NodeMonitor {

    @DataBoundConstructor
    public ConnectionMonitor() {}

    @Extension
    @Symbol("connectionMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<ConnectionInfo> {

        /**
         * The connection is determined here on the controller from the launch method and handed to
         * the callable, which adds the node's IP address and reports it back once the node is online.
         */
        @Override
        protected MasterToSlaveCallable<ConnectionInfo, IOException> createCallable(Computer c) {
            if (c instanceof SlaveComputer slave) {
                return new GetConnectionInfo(ConnectionInfo.of(slave.getLauncher(), false));
            }
            return new GetConnectionInfo(ConnectionInfo.of(null, true));
        }

        @Override
        public String getDisplayName() {
            return Messages.connectionMonitor_displayName();
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

    private static final class GetConnectionInfo extends MasterToSlaveCallable<ConnectionInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private final ConnectionInfo info;

        GetConnectionInfo(ConnectionInfo info) {
            this.info = info;
        }

        @Override
        public ConnectionInfo call() {
            // Runs on the node, so the address is the one of the node itself.
            return info.withHost(IpAddressFinder.find(), RemoteTarget.Family.fromOsName(System.getProperty("os.name")));
        }
    }
}
