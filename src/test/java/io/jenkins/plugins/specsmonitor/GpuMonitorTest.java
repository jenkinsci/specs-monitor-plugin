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
class GpuMonitorTest {

    @Test
    void monitorIsRegistered(JenkinsRule j) {
        GpuMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(GpuMonitor.DescriptorImpl.class);
        assertNotNull(descriptor);
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof GpuMonitor.DescriptorImpl));
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    @Test
    void callableReturnsResultWhenNoGpuIsDetected(JenkinsRule j) throws Exception {
        GpuMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(GpuMonitor.DescriptorImpl.class);

        GpuInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertNotNull(info);
        assertFalse(info.getDisplay().isBlank());
    }
}
