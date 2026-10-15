package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.node_monitors.NodeMonitor;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ConnectionMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        assertNotNull(ExtensionList.lookupSingleton(ConnectionMonitor.DescriptorImpl.class));
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof ConnectionMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        ConnectionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(ConnectionMonitor.DescriptorImpl.class);
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    @Test
    void builtInNode(JenkinsRule j) {
        assertEquals("Built-in", ConnectionInfo.of(null, true).getDisplay());
    }

    @Test
    void inboundWebSocket(JenkinsRule j) {
        JNLPLauncher launcher = new JNLPLauncher();
        launcher.setWebSocket(true);
        assertEquals("WebSocket", ConnectionInfo.of(launcher, false).getDisplay());
    }

    @Test
    void inboundTcp(JenkinsRule j) {
        assertEquals("JNLP", ConnectionInfo.of(new JNLPLauncher(), false).getDisplay());
    }

    @Test
    void noLauncherIsNotAvailable(JenkinsRule j) {
        assertEquals("N/A", ConnectionInfo.of(null, false).getDisplay());
    }

    @Test
    void callableReportsBuiltInNode(JenkinsRule j) throws Exception {
        ConnectionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(ConnectionMonitor.DescriptorImpl.class);

        ConnectionInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertEquals("Built-in", info.getDisplay());
    }

    @Test
    void callableReportsAgentLauncher(JenkinsRule j, @TempDir Path agentDir) throws Exception {
        JNLPLauncher launcher = new JNLPLauncher();
        launcher.setWebSocket(true);
        DumbSlave agent = new DumbSlave("agent", agentDir.toString(), launcher);
        j.jenkins.addNode(agent);
        ConnectionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(ConnectionMonitor.DescriptorImpl.class);

        ConnectionInfo info = descriptor.createCallable(agent.toComputer()).call();

        assertEquals("WebSocket", info.getDisplay());
    }
}
