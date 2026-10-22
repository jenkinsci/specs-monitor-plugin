package io.jenkins.plugins.specsmonitor;

import hudson.ExtensionList;
import hudson.model.Computer;
import hudson.model.ComputerSet;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.lang.reflect.Method;
import java.util.List;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.JenkinsRule;

/** Helpers to enable a monitor, wait for its data and render the node list. */
final class ColumnTestSupport {

    private ColumnTestSupport() {}

    /**
     * Enables the monitor. Monitors are enabled by default, so this replaces the default instance
     * instead of adding a second column.
     */
    static void enable(NodeMonitor monitor) throws Exception {
        ComputerSet.getMonitors().remove(monitor.getClass());
        ComputerSet.getMonitors().add(monitor);
    }

    /** Waits until the monitor has data for the computer, and returns it. */
    static <T> T awaitData(Class<? extends AbstractAsyncNodeMonitorDescriptor<T>> descriptor, Computer computer)
            throws Exception {
        AbstractAsyncNodeMonitorDescriptor<T> d = ExtensionList.lookupSingleton(descriptor);
        // triggerUpdate() is not public, but it is what the periodic monitoring calls.
        Method triggerUpdate = AbstractNodeMonitorDescriptor.class.getDeclaredMethod("triggerUpdate");
        triggerUpdate.setAccessible(true);
        for (int i = 0; i < 300 && d.get(computer) == null; i++) {
            triggerUpdate.invoke(d);
            Thread.sleep(100);
        }
        return d.get(computer);
    }

    static HtmlPage renderNodeList(JenkinsRule j) throws Exception {
        return j.createWebClient().goTo("computer/");
    }

    /** The cells of the node list in the row of the node with this name whose tooltip contains the text. */
    static List<HtmlElement> cellsInRow(HtmlPage page, String nodeName, String tooltipText) {
        return page.getByXPath(
                "//tr[contains(@id, '" + nodeName + "')]//td[contains(@tooltip, '" + tooltipText + "')]");
    }
}
