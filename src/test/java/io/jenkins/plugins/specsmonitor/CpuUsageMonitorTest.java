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
class CpuUsageMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(CpuUsageMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof CpuUsageMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        CpuUsageMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(CpuUsageMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    /** The JVMs used on CI (Windows, Linux, macOS) can all measure CPU usage. */
    @Test
    void callableReturnsUsageOnAnyOs(JenkinsRule j) throws Exception {
        CpuUsageMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(CpuUsageMonitor.DescriptorImpl.class);

        CpuUsageInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertNotNull(info);
        assertTrue(info.getSystemPercent() >= 0 && info.getSystemPercent() <= 100);
    }

    @Test
    void isHighWhenUsageReachesThreshold(JenkinsRule j) {
        CpuUsageMonitor monitor = new CpuUsageMonitor();
        monitor.setWarnAbovePercent(90);

        assertTrue(monitor.isHigh(new CpuUsageInfo(91, 1)));
        assertTrue(monitor.isHigh(new CpuUsageInfo(90, 1)));
        assertFalse(monitor.isHigh(new CpuUsageInfo(89, 1)));
        assertFalse(monitor.isHigh(new CpuUsageInfo(-1, -1)));
        assertFalse(monitor.isHigh(null));
    }

    @Test
    void thresholdOfHundredNeverWarns(JenkinsRule j) {
        CpuUsageMonitor monitor = new CpuUsageMonitor();
        monitor.setWarnAbovePercent(150);

        assertFalse(monitor.isHigh(new CpuUsageInfo(100, 1)));
    }
}
