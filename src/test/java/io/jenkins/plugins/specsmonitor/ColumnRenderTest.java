package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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

    private static final String WARNING_SYMBOL = "⚠";

    @Test
    void jvmVersionShowsTooltip(JenkinsRule j) throws Exception {
        assertShowsTooltip(j, new JvmVersionMonitor(), JvmVersionMonitor.DescriptorImpl.class, VersionInfo::getTooltip);
    }

    @Test
    void remotingVersionShowsTooltip(JenkinsRule j) throws Exception {
        assertShowsTooltip(
                j, new RemotingVersionMonitor(), RemotingVersionMonitor.DescriptorImpl.class, VersionInfo::getTooltip);
    }

    /** Git may not be installed where the tests run, in which case there is no tooltip to show. */
    @Test
    void gitVersionShowsTooltip(JenkinsRule j) throws Exception {
        assumeGitIsInstalled(j);

        assertShowsTooltip(j, new GitVersionMonitor(), GitVersionMonitor.DescriptorImpl.class, VersionInfo::getTooltip);
    }

    @Test
    void freeRamShowsTooltip(JenkinsRule j) throws Exception {
        assertShowsTooltip(j, new MemoryMonitor(), MemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip);
    }

    @Test
    void freeJvmMemoryShowsTooltip(JenkinsRule j) throws Exception {
        assertShowsTooltip(j, new JvmMemoryMonitor(), JvmMemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip);
    }

    @Test
    void freeRamWarnsWhenBelowThreshold(JenkinsRule j) throws Exception {
        MemoryMonitor always = new MemoryMonitor();
        always.setWarnBelowPercent(100);

        assertTrue(cellWarns(j, always, MemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip));
    }

    @Test
    void freeRamDoesNotWarnAboveThreshold(JenkinsRule j) throws Exception {
        MemoryMonitor never = new MemoryMonitor();
        never.setWarnBelowPercent(0);

        assertFalse(cellWarns(j, never, MemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip));
    }

    @Test
    void freeJvmMemoryWarnsWhenBelowThreshold(JenkinsRule j) throws Exception {
        JvmMemoryMonitor always = new JvmMemoryMonitor();
        always.setWarnBelowPercent(100);

        assertTrue(cellWarns(j, always, JvmMemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip));
    }

    @Test
    void freeJvmMemoryDoesNotWarnAboveThreshold(JenkinsRule j) throws Exception {
        JvmMemoryMonitor never = new JvmMemoryMonitor();
        never.setWarnBelowPercent(0);

        assertFalse(cellWarns(j, never, JvmMemoryMonitor.DescriptorImpl.class, MemoryInfo::getTooltip));
    }

    /** Checks that the node list has the tooltip of the data the monitor collected for the built-in node. */
    private static <T> void assertShowsTooltip(
            JenkinsRule j,
            NodeMonitor monitor,
            Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor,
            Function<T, String> tooltip)
            throws Exception {
        String html = renderPage(j, monitor, descriptor).getWebResponse().getContentAsString();

        String expected =
                tooltip.apply(ExtensionList.lookupSingleton(descriptor).get(j.jenkins.toComputer()));
        assertFalse(expected.isEmpty(), "the monitor reported no tooltip");
        assertTrue(html.contains("tooltip=\"" + expected + "\""), html);
    }

    /**
     * Whether the monitor's cell for the built-in node shows the warning symbol. Only that cell is
     * checked, because other monitors on the page can show a warning of their own.
     */
    private static <T> boolean cellWarns(
            JenkinsRule j,
            NodeMonitor monitor,
            Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor,
            Function<T, String> tooltip)
            throws Exception {
        HtmlPage page = renderPage(j, monitor, descriptor);
        String expected =
                tooltip.apply(ExtensionList.lookupSingleton(descriptor).get(j.jenkins.toComputer()));
        List<HtmlElement> cells = page.getByXPath("//td[@tooltip='" + expected + "']");
        assertEquals(1, cells.size(), "expected exactly one cell with the tooltip " + expected);
        return cells.get(0).getTextContent().contains(WARNING_SYMBOL);
    }

    /** Enables the monitor, waits until it has data for the built-in node and returns the node list. */
    private static <T> HtmlPage renderPage(
            JenkinsRule j, NodeMonitor monitor, Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor)
            throws Exception {
        ColumnTestSupport.enable(monitor);
        ColumnTestSupport.awaitData(descriptor, j.jenkins.toComputer());
        return ColumnTestSupport.renderNodeList(j);
    }

    /** The version cell of the built-in node, when the controller is made to look newer than it. */
    private static HtmlElement versionCellWithNewerController(
            JenkinsRule j,
            NodeMonitor monitor,
            Class<? extends AbstractAsyncNodeMonitorDescriptor<VersionInfo>> descriptor)
            throws Exception {
        HtmlPage page = renderPage(j, monitor, descriptor);
        String tooltip = ExtensionList.lookupSingleton(descriptor)
                .get(j.jenkins.toComputer())
                .getTooltip();
        List<HtmlElement> cells = page.getByXPath("//td[starts-with(@tooltip, '" + tooltip + "')]");
        assertEquals(1, cells.size(), "expected exactly one cell with the tooltip " + tooltip);
        return cells.get(0);
    }

    @Test
    void jvmVersionWarnsWhenControllerIsNewer(JenkinsRule j) throws Exception {
        JvmVersionMonitor monitor = new JvmVersionMonitor();
        monitor.setControllerVersionOverride(VersionInfo.fromJvm(Runtime.Version.parse("99.0.1+1")));

        HtmlElement cell = versionCellWithNewerController(j, monitor, JvmVersionMonitor.DescriptorImpl.class);

        assertTrue(cell.getTextContent().contains(WARNING_SYMBOL));
        assertTrue(cell.getAttribute("tooltip").endsWith("(older than the controller: 99.0.1+1)"));
    }

    @Test
    void remotingVersionWarnsWhenControllerIsNewer(JenkinsRule j) throws Exception {
        RemotingVersionMonitor monitor = new RemotingVersionMonitor();
        monitor.setControllerVersionOverride(VersionInfo.fromRemoting("999999.v1234"));

        HtmlElement cell = versionCellWithNewerController(j, monitor, RemotingVersionMonitor.DescriptorImpl.class);

        assertTrue(cell.getTextContent().contains(WARNING_SYMBOL));
        assertTrue(cell.getAttribute("tooltip").endsWith("(older than the controller: 999999.v1234)"));
    }

    @Test
    void gitVersionWarnsWhenControllerIsNewer(JenkinsRule j) throws Exception {
        assumeGitIsInstalled(j);
        GitVersionMonitor monitor = new GitVersionMonitor();
        monitor.setControllerVersionOverride(VersionInfo.fromGit("git version 99.0.0"));

        HtmlElement cell = versionCellWithNewerController(j, monitor, GitVersionMonitor.DescriptorImpl.class);

        assertTrue(cell.getTextContent().contains(WARNING_SYMBOL));
        assertTrue(cell.getAttribute("tooltip").endsWith("(older than the controller: 99.0.0)"));
    }

    @Test
    void versionsDoNotWarnWhenNodeMatchesController(JenkinsRule j) throws Exception {
        assumeGitIsInstalled(j);
        for (NodeMonitor monitor :
                List.of(new JvmVersionMonitor(), new RemotingVersionMonitor(), new GitVersionMonitor())) {
            ColumnTestSupport.enable(monitor);
        }
        ColumnTestSupport.awaitData(JvmVersionMonitor.DescriptorImpl.class, j.jenkins.toComputer());
        ColumnTestSupport.awaitData(RemotingVersionMonitor.DescriptorImpl.class, j.jenkins.toComputer());
        ColumnTestSupport.awaitData(GitVersionMonitor.DescriptorImpl.class, j.jenkins.toComputer());

        String html = ColumnTestSupport.renderNodeList(j).getWebResponse().getContentAsString();

        assertFalse(html.contains("older than the controller"), html);
    }

    private static void assumeGitIsInstalled(JenkinsRule j) throws Exception {
        GitVersionMonitor.DescriptorImpl descriptor =
                ExtensionList.lookupSingleton(GitVersionMonitor.DescriptorImpl.class);
        assumeFalse(
                descriptor
                        .createCallable(j.jenkins.toComputer())
                        .call()
                        .getTooltip()
                        .isEmpty(),
                "Git not found");
    }
}
