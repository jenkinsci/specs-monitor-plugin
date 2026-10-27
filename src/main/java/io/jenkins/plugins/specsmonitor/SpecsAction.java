package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TransientComputerActionFactory;
import hudson.remoting.VirtualChannel;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;

/** Adds a "Specs" page to the computer page of a node that lists its RAM and the drives and volumes it can use. */
public class SpecsAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(SpecsAction.class.getName());

    private static final long RAM_TIMEOUT_SECONDS = 15;

    private final Computer computer;

    public SpecsAction(Computer computer) {
        this.computer = computer;
    }

    public Computer getComputer() {
        return computer;
    }

    @Override
    public String getIconFileName() {
        return computer.hasPermission(Computer.EXTENDED_READ) ? "symbol-computer" : null;
    }

    @Override
    public String getDisplayName() {
        return "Specs";
    }

    @Override
    public String getUrlName() {
        return "specs";
    }

    /**
     * Asks the node for its drives and volumes. Returns {@code null} if the node is offline or
     * does not answer in time, since there is no meaningful last known value.
     */
    public List<StorageVolumes.Volume> getVolumes() {
        // The node stops waiting for slow volumes itself, the extra time covers a node that does not answer.
        return callOnNode(new GetVolumes(workspacePath()), StorageVolumes.TIMEOUT_SECONDS + 8, "storage");
    }

    /** Asks the node for its RAM. Returns {@code null} if the node is offline or does not answer in time. */
    public RamSpecs getRam() {
        return callOnNode(new GetRam(), RAM_TIMEOUT_SECONDS, "RAM");
    }

    private <T> T callOnNode(MasterToSlaveCallable<T, IOException> callable, long timeoutSeconds, String what) {
        computer.checkPermission(Computer.EXTENDED_READ);
        VirtualChannel channel = computer.getChannel();
        if (channel == null) {
            return null;
        }
        Future<T> lookup = null;
        try {
            lookup = channel.callAsync(callable);
            return lookup.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (IOException | ExecutionException | TimeoutException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not read the " + what + " of " + computer.getName());
            if (lookup != null) {
                lookup.cancel(true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private String workspacePath() {
        Node node = computer.getNode();
        FilePath root = node == null ? null : node.getRootPath();
        return root == null ? null : root.getRemote();
    }

    /** Runs on the node itself, so it reports the storage the node can reach. */
    private static final class GetVolumes extends MasterToSlaveCallable<List<StorageVolumes.Volume>, IOException> {
        private static final long serialVersionUID = 1L;

        private final String workspacePath;

        GetVolumes(String workspacePath) {
            this.workspacePath = workspacePath;
        }

        @Override
        public List<StorageVolumes.Volume> call() {
            return StorageVolumes.collect(workspacePath);
        }
    }

    /** Runs on the node itself, so it reports the RAM the node has. */
    private static final class GetRam extends MasterToSlaveCallable<RamSpecs, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public RamSpecs call() {
            return RamSpecs.collect();
        }
    }

    @Extension
    public static class Factory extends TransientComputerActionFactory {
        @Override
        public Collection<? extends Action> createFor(Computer target) {
            return List.of(new SpecsAction(target));
        }
    }
}
