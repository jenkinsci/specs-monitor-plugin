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
class MemoryMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(MemoryMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof MemoryMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        MemoryMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(MemoryMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    /**
     * Falls back to the JVM, so this must report a positive value on any CI
     * operating system.
     */
    @Test
    void callableReturnsPositiveTotalOnAnyOs(JenkinsRule j) throws Exception {
        MemoryMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(MemoryMonitor.DescriptorImpl.class);

        MemoryInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertNotNull(info);
        assertTrue(info.getTotalBytes() > 0);
    }

    @Test
    void isLowWhenUsableBelowThreshold(JenkinsRule j) {
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.setWarnBelowPercent(10);

        assertTrue(monitor.isLow(new MemoryInfo(1000, 500, 99)));
        assertFalse(monitor.isLow(new MemoryInfo(1000, 50, 100)));
        assertTrue(monitor.isLow(new MemoryInfo(1000, 99, -1)));
        assertFalse(monitor.isLow(new MemoryInfo(1000)));
        assertFalse(monitor.isLow(null));
    }
}
