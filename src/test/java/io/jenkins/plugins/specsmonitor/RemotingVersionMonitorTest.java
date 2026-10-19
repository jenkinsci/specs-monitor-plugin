package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Launcher;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RemotingVersionMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(RemotingVersionMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof RemotingVersionMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        RemotingVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(RemotingVersionMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    @Test
    void callableReportsTheRemotingVersion(JenkinsRule j) throws Exception {
        RemotingVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(RemotingVersionMonitor.DescriptorImpl.class);

        VersionInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertEquals(Launcher.VERSION, info.getTooltip());
    }
}
