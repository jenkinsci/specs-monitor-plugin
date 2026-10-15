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
class JvmMemoryMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(JvmMemoryMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof JvmMemoryMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        JvmMemoryMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(JvmMemoryMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    @Test
    void callableReportsFreeOutOfTotal(JenkinsRule j) throws Exception {
        JvmMemoryMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(JvmMemoryMonitor.DescriptorImpl.class);

        MemoryInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertTrue(info.getTotalBytes() > 0);
        assertTrue(info.getFreeBytes() >= 0);
        assertTrue(info.getFreeBytes() <= info.getTotalBytes());
        assertTrue(info.getTooltip().contains(" free of "));
    }
}
