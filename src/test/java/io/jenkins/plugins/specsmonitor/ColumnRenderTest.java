package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import hudson.ExtensionList;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.util.List;
import java.util.function.Function;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Renders the node list for the columns that call their monitor from the template, to check that
 * the template can reach the monitor, shows the tooltip and, where there is one, the warning.
 * The unit tests of the monitors cannot catch a broken template.
 */
@WithJenkins
class ColumnRenderTest {

    @Test
    void freeRamShowsTooltip(JenkinsRule j) throws Exception {
        assertShowsTooltip(j, new MemoryMonitor(), MemoryMonitor.DescriptorImpl.class, i -> i.getTooltip());
    }

    @Test
    void freeJvmMemoryShowsTooltip(JenkinsRule j) throws Exception {
        assertShowsTooltip(j, new JvmMemoryMonitor(), JvmMemoryMonitor.DescriptorImpl.class, i -> i.getTooltip());
    }

    @Test
    void freeRamWarnsWhenBelowThreshold(JenkinsRule j) throws Exception {
        MemoryMonitor always = new MemoryMonitor();
        always.setWarnBelowPercent(100);

        assertTrue(cellWarns(j, always, MemoryMonitor.DescriptorImpl.class, i -> i.getTooltip()));
    }

    @Test
    void freeRamDoesNotWarnAboveThreshold(JenkinsRule j) throws Exception {
        MemoryMonitor never = new MemoryMonitor();
        never.setWarnBelowPercent(0);

        assertFalse(cellWarns(j, never, MemoryMonitor.DescriptorImpl.class, i -> i.getTooltip()));
    }

    @Test
    void freeJvmMemoryWarnsWhenBelowThreshold(JenkinsRule j) throws Exception {
        JvmMemoryMonitor always = new JvmMemoryMonitor();
        always.setWarnBelowPercent(100);

        assertTrue(cellWarns(j, always, JvmMemoryMonitor.DescriptorImpl.class, i -> i.getTooltip()));
    }

    @Test
    void freeJvmMemoryDoesNotWarnAboveThreshold(JenkinsRule j) throws Exception {
        JvmMemoryMonitor never = new JvmMemoryMonitor();
        never.setWarnBelowPercent(0);

        assertFalse(cellWarns(j, never, JvmMemoryMonitor.DescriptorImpl.class, i -> i.getTooltip()));
    }

    @Test
    void cpuUsageShowsTooltip(JenkinsRule j) throws Exception {
        ColumnTestSupport.assumeCpuUsageAvailable(j);
        assertShowsTooltip(j, new CpuUsageMonitor(), CpuUsageMonitor.DescriptorImpl.class, i -> i.getTooltip());
    }

    @Test
    void cpuUsageWarnsWhenAboveThreshold(JenkinsRule j) throws Exception {
        ColumnTestSupport.assumeCpuUsageAvailable(j);
        CpuUsageMonitor always = new CpuUsageMonitor();
        always.setWarnAbovePercent(0);

        assertTrue(cellWarns(j, always, CpuUsageMonitor.DescriptorImpl.class, i -> i.getTooltip()));
    }

    @Test
    void cpuUsageDoesNotWarnWithThresholdOfHundred(JenkinsRule j) throws Exception {
        CpuUsageMonitor never = new CpuUsageMonitor();
        never.setWarnAbovePercent(100);

        assertFalse(cellWarns(j, never, CpuUsageMonitor.DescriptorImpl.class, i -> i.getTooltip()));
    }

    /** Checks that the node list has the tooltip of the data the monitor collected for the built-in node. */
    private static <T> void assertShowsTooltip(
            JenkinsRule j,
            NodeMonitor monitor,
            Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor,
            Function<T, String> tooltip)
            throws Exception {
        String html = renderPage(j, monitor, descriptor).getWebResponse().getContentAsString();
        String expected = null;
        // Free JVM memory and CPU usage change constantly and the monitor can refresh between rendering the page
        // and reading the data, so retry until both agree.
        for (int i = 0; i < 10; i++) {
            if (i > 0) {
                html = ColumnTestSupport.renderNodeList(j).getWebResponse().getContentAsString();
            }
            expected = tooltip.apply(ExtensionList.lookupSingleton(descriptor).get(j.jenkins.toComputer()));
            assertFalse(expected.isEmpty(), "the monitor reported no tooltip");
            if (html.contains("tooltip=\"" + expected + "\"")) {
                return;
            }
        }
        fail("the node list never showed the tooltip " + expected + ":\n" + html);
    }

    /**
     * Whether the monitor's cell for the built-in node is highlighted with the warning class. Only that cell is
     * checked, because other monitors on the page can show a warning of their own.
     */
    private static <T> boolean cellWarns(
            JenkinsRule j,
            NodeMonitor monitor,
            Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor,
            Function<T, String> tooltip)
            throws Exception {
        HtmlPage page = renderPage(j, monitor, descriptor);
        String expected = null;
        List<HtmlElement> cells = List.of();
        // Free JVM memory and CPU usage change constantly and the monitor can refresh between rendering the page
        // and reading the data, so retry until both agree.
        for (int i = 0; i < 10 && cells.size() != 1; i++) {
            if (i > 0) {
                page = ColumnTestSupport.renderNodeList(j);
            }
            expected = tooltip.apply(ExtensionList.lookupSingleton(descriptor).get(j.jenkins.toComputer()));
            cells = page.getByXPath("//td[@tooltip='" + expected + "']");
        }
        assertEquals(1, cells.size(), "expected exactly one cell with the tooltip " + expected);
        return !cells.get(0).getByXPath(".//span[@class='warning']").isEmpty();
    }

    /** Enables the monitor, waits until it has data for the built-in node and returns the node list. */
    private static <T> HtmlPage renderPage(
            JenkinsRule j, NodeMonitor monitor, Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor)
            throws Exception {
        ColumnTestSupport.enable(monitor);
        ColumnTestSupport.awaitData(descriptor, j.jenkins.toComputer());
        return ColumnTestSupport.renderNodeList(j);
    }
}
