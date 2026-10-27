package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.model.TransientComputerActionFactory;
import hudson.remoting.VirtualChannel;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * Adds a "Remote access" page to the computer page of a node with a button that
 * opens a Remote Desktop session (Windows) or an SSH session (Linux and macOS).
 */
public class RemoteAccessAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(RemoteAccessAction.class.getName());

    private final Computer computer;

    public RemoteAccessAction(Computer computer) {
        this.computer = computer;
    }

    public Computer getComputer() {
        return computer;
    }

    @Override
    public String getIconFileName() {
        return computer.hasPermission(Computer.CONFIGURE) ? "symbol-terminal" : null;
    }

    @Override
    public String getDisplayName() {
        return "Remote Access";
    }

    @Override
    public String getUrlName() {
        return "remote-access";
    }

    /**
     * Asks the node for its operating system and IP address. If the node is offline or does
     * not answer, falls back to what the Connection monitor last collected, and returns
     * {@code null} if there is none.
     */
    public RemoteTarget getTarget() {
        computer.checkPermission(Computer.CONFIGURE);
        VirtualChannel channel = computer.getChannel();
        if (channel != null) {
            try {
                return channel.call(new GetRemoteTarget());
            } catch (IOException e) {
                LOGGER.log(Level.FINE, e, () -> "Could not read the address of " + computer.getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return lastKnownTarget();
    }

    /**
     * Asks the node which users are logged in right now. Returns {@code null} if the node is
     * offline or does not answer, since there is no meaningful last known value.
     */
    public List<ConnectedUsers.Session> getConnectedUsers() {
        computer.checkPermission(Computer.CONFIGURE);
        VirtualChannel channel = computer.getChannel();
        if (channel == null) {
            return null;
        }
        try {
            return channel.call(new GetConnectedUsers());
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not read the users of " + computer.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /** The address the Connection monitor last collected from the node, which it keeps while offline. */
    private RemoteTarget lastKnownTarget() {
        ConnectionInfo info = ExtensionList.lookupSingleton(ConnectionMonitor.DescriptorImpl.class)
                .get(computer);
        return info == null ? null : info.getRemoteTarget();
    }

    /** Sends an {@code .rdp} file that opens Remote Desktop Connection for a Windows node. */
    public void doRdp(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        RemoteTarget target = getTarget();
        if (target == null || !target.isWindows()) {
            rsp.sendError(StaplerResponse2.SC_NOT_FOUND, "No Remote Desktop target");
            return;
        }
        String name = computer.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        String fileName = (name.isEmpty() ? "built-in" : name) + ".rdp";
        rsp.setContentType("application/x-rdp");
        rsp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        rsp.getWriter().print(target.toRdpFile());
    }

    /** Runs on the node itself, so it reports the node's own system and address. */
    private static final class GetRemoteTarget extends MasterToSlaveCallable<RemoteTarget, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public RemoteTarget call() {
            return new RemoteTarget(
                    RemoteTarget.Family.fromOsName(System.getProperty("os.name")), IpAddressFinder.find());
        }
    }

    /** Runs on the node itself, so it reports the users logged in to the node. */
    private static final class GetConnectedUsers
            extends MasterToSlaveCallable<List<ConnectedUsers.Session>, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public List<ConnectedUsers.Session> call() {
            return ConnectedUsers.collect(RemoteTarget.Family.fromOsName(System.getProperty("os.name")));
        }
    }

    @Extension
    public static class Factory extends TransientComputerActionFactory {
        @Override
        public Collection<? extends Action> createFor(Computer target) {
            return List.of(new RemoteAccessAction(target));
        }
    }
}
