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

/** Adds a "Storage" page to the computer page of a node that lists the drives and volumes it can use. */
public class StorageAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(StorageAction.class.getName());

    private final Computer computer;

    public StorageAction(Computer computer) {
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
        return "Storage";
    }

    @Override
    public String getUrlName() {
        return "storage";
    }

    /**
     * Asks the node for its drives and volumes. Returns {@code null} if the node is offline or
     * does not answer in time, since there is no meaningful last known value.
     */
    public List<StorageVolumes.Volume> getVolumes() {
        computer.checkPermission(Computer.EXTENDED_READ);
        VirtualChannel channel = computer.getChannel();
        if (channel == null) {
            return null;
        }
        Future<List<StorageVolumes.Volume>> lookup = null;
        try {
            lookup = channel.callAsync(new GetVolumes(workspacePath()));
            // The node stops waiting for slow volumes itself, this covers a node that does not answer.
            return lookup.get(StorageVolumes.TIMEOUT_SECONDS + 8, TimeUnit.SECONDS);
        } catch (IOException | ExecutionException | TimeoutException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not read the storage of " + computer.getName());
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

    @Extension
    public static class Factory extends TransientComputerActionFactory {
        @Override
        public Collection<? extends Action> createFor(Computer target) {
            return List.of(new StorageAction(target));
        }
    }
}
