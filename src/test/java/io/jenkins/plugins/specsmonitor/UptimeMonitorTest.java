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
class UptimeMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(UptimeMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof UptimeMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        UptimeMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(UptimeMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    /** Falls back to the JVM, so this must report a value on any CI operating system. */
    @Test
    void callableReturnsUptimeOnAnyOs(JenkinsRule j) throws Exception {
        UptimeMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(UptimeMonitor.DescriptorImpl.class);

        UptimeInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertTrue(info.getSeconds() >= 0);
    }
}
