package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import hudson.ExtensionList;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.slaves.DumbSlave;
import java.util.List;
import java.util.function.Function;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Runs the monitors on a real agent, which the harness starts as a separate JVM connected over
 * remoting. The monitors then run on the agent and send their result back, which the built-in
 * node never does.
 */
@WithJenkins
class AgentColumnRenderTest {

    private static final String WARNING_SYMBOL = "⚠";

    @Test
    void jvmVersionIsReportedByAgent(JenkinsRule j) throws Exception {
        assertAgentRowShowsTooltip(
                j, new JvmVersionMonitor(), JvmVersionMonitor.DescriptorImpl.class, VersionInfo::getTooltip);
    }

    @Test
    void remotingVersionIsReportedByAgent(JenkinsRule j) throws Exception {
        assertAgentRowShowsTooltip(
                j, new RemotingVersionMonitor(), RemotingVersionMonitor.DescriptorImpl.class, VersionInfo::getTooltip);
    }

    @Test
    void gitVersionIsReportedByAgent(JenkinsRule j) throws Exception {
        GitVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(GitVersionMonitor.DescriptorImpl.class);
        assumeFalse(
                descriptor
                        .createCallable(j.jenkins.toComputer())
                        .call()
                        .getTooltip()
                        .isEmpty(),
                "Git not found");

        assertAgentRowShowsTooltip(
                j, new GitVersionMonitor(), GitVersionMonitor.DescriptorImpl.class, VersionInfo::getTooltip);
    }

    @Test
    void freeRamIsReportedByAgent(JenkinsRule j) throws Exception {
        assertAgentRowShowsTooltip(j, new MemoryMonitor(), MemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip);
    }

    @Test
    void freeJvmMemoryIsReportedByAgent(JenkinsRule j) throws Exception {
        assertAgentRowShowsTooltip(
                j, new JvmMemoryMonitor(), JvmMemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip);
    }

    @Test
    void agentWarnsWhenOlderThanController(JenkinsRule j) throws Exception {
        JvmVersionMonitor monitor = new JvmVersionMonitor();
        monitor.setControllerVersionOverride(VersionInfo.fromJvm(Runtime.Version.parse("99.0.1+1")));
        DumbSlave agent = j.createOnlineSlave();
        ColumnTestSupport.enable(monitor);
        VersionInfo info = ColumnTestSupport.awaitData(JvmVersionMonitor.DescriptorImpl.class, agent.toComputer());
        assertNotNull(info, "the agent reported no JVM version");

        List<HtmlElement> cells = ColumnTestSupport.cellsInRow(
                ColumnTestSupport.renderNodeList(j), agent.getNodeName(), info.getTooltip());

        assertEquals(1, cells.size());
        assertTrue(cells.get(0).getTextContent().contains(WARNING_SYMBOL));
    }

    /** An agent that is not connected has no data, which must render as N/A and not as an error. */
    @Test
    void offlineAgentShowsNotAvailable(JenkinsRule j) throws Exception {
        DumbSlave agent = j.createSlave();
        ColumnTestSupport.enable(new JvmVersionMonitor());

        HtmlPage page = ColumnTestSupport.renderNodeList(j);

        List<HtmlElement> cells = page.getByXPath("//tr[contains(@id, '" + agent.getNodeName() + "')]//td");
        assertTrue(cells.stream().anyMatch(c -> c.getTextContent().trim().equals("N/A")));
    }

    /** Starts an agent and checks that its row shows the tooltip of the data the agent sent back. */
    private static <T> void assertAgentRowShowsTooltip(
            JenkinsRule j,
            NodeMonitor monitor,
            Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor,
            Function<T, String> tooltip)
            throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.toComputer();
        ColumnTestSupport.enable(monitor);
        T data = ColumnTestSupport.awaitData(descriptor, computer);
        assertNotNull(data, "the agent reported no data");

        String expected = tooltip.apply(data);
        assertFalse(expected.isEmpty(), "the agent reported no tooltip");
        List<HtmlElement> cells =
                ColumnTestSupport.cellsInRow(ColumnTestSupport.renderNodeList(j), agent.getNodeName(), expected);
        assertEquals(1, cells.size(), "expected the agent's row to show the tooltip " + expected);
    }
}
