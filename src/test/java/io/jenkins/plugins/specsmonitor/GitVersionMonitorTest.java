package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.node_monitors.NodeMonitor;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitVersionMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(GitVersionMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof GitVersionMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        GitVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(GitVersionMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    /** Git may not be installed on the machine running the tests, so it only has to not fail. */
    @Test
    void callableReturnsAVersionInfo(JenkinsRule j) throws Exception {
        GitVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(GitVersionMonitor.DescriptorImpl.class);

        VersionInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertNotNull(info);
        assertNotNull(info.getDisplay());
    }

    @Test
    void unknownVersionIsNeverOutdated(JenkinsRule j) {
        GitVersionMonitor monitor = new GitVersionMonitor();

        assertFalse(monitor.isOutdated(null));
        assertFalse(monitor.isOutdated(VersionInfo.fromGit("")));
    }
}
