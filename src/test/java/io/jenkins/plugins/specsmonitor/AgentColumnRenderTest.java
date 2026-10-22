package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void freeRamIsReportedByAgent(JenkinsRule j) throws Exception {
        assertAgentRowShowsTooltip(j, new MemoryMonitor(), MemoryMonitor.DescriptorImpl.class, i -> i.getTooltip());
    }

    @Test
    void freeJvmMemoryIsReportedByAgent(JenkinsRule j) throws Exception {
        // The free heap changes between the sample taken here and the one on the page, so only the
        // maximum heap, which is fixed for the agent JVM, is compared.
        assertAgentRowShowsTooltip(j, new JvmMemoryMonitor(), JvmMemoryMonitor.DescriptorImpl.class, i -> {
            String tooltip = i.getTooltip();
            return tooltip.substring(tooltip.indexOf(" free of "));
        });
    }

    /** An agent that is not connected has no data, which must render as N/A and not as an error. */
    @Test
    void offlineAgentShowsNotAvailable(JenkinsRule j) throws Exception {
        DumbSlave agent = j.createSlave();
        ColumnTestSupport.enable(new MemoryMonitor());

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
