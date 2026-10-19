package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.node_monitors.NodeMonitor;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JvmVersionMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(JvmVersionMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof JvmVersionMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        JvmVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(JvmVersionMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    @Test
    void callableReportsTheJvmVersion(JenkinsRule j) throws Exception {
        JvmVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(JvmVersionMonitor.DescriptorImpl.class);

        VersionInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertEquals(Runtime.version().toString(), info.getTooltip());
    }
}
