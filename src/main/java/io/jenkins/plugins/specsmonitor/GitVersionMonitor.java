package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Node monitor that reports the version of Git installed on each node, with the full version
 * string as a tooltip.
 */
public class GitVersionMonitor extends NodeMonitor {

    private static final long CONTROLLER_CACHE_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private static volatile VersionInfo cachedControllerVersion;
    private static volatile long cachedAt;

    @DataBoundConstructor
    public GitVersionMonitor() {}

    /**
     * The version of Git on the controller. The result is cached for a few minutes, because
     * this is called for every node each time the node list is rendered.
     */
    private static VersionInfo controllerVersion() {
        VersionInfo cached = cachedControllerVersion;
        if (cached == null || System.currentTimeMillis() - cachedAt > CONTROLLER_CACHE_MILLIS) {
            cached = readGitVersion();
            cachedControllerVersion = cached;
            cachedAt = System.currentTimeMillis();
        }
        return cached;
    }

    private static VersionInfo readGitVersion() {
        return VersionInfo.fromGit(CommandRunner.tryRun("git", "--version"));
    }

    /** Whether the node has an older Git than the controller. */
    public boolean isOutdated(VersionInfo info) {
        return info != null && info.isOlderThan(controllerVersion());
    }

    /** The full version, with the controller's version added if the node is outdated. */
    public String getTooltip(VersionInfo info) {
        if (info == null) {
            return "";
        }
        return isOutdated(info)
                ? info.getTooltip() + " (older than the controller: "
                        + controllerVersion().getTooltip() + ")"
                : info.getTooltip();
    }

    @Extension
    @Symbol("gitVersionMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<VersionInfo> {

        @Override
        protected MasterToSlaveCallable<VersionInfo, IOException> createCallable(Computer c) {
            return new GetGitVersion();
        }

        @Override
        public String getDisplayName() {
            return Messages.gitVersionMonitor_displayName();
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

    /** Runs on the node itself, so it reports the Git found on the node's path. */
    private static final class GetGitVersion extends MasterToSlaveCallable<VersionInfo, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public VersionInfo call() {
            return readGitVersion();
        }
    }
}
