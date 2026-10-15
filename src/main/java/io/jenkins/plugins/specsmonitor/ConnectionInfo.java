package io.jenkins.plugins.specsmonitor;

import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.JNLPLauncher;
import java.io.Serializable;
import java.util.Locale;

/** How a node is connected to the controller, e.g. WebSocket, JNLP (TCP) or SSH. */
public class ConnectionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String display;
    private final String tooltip;

    public ConnectionInfo(String display, String tooltip) {
        this.display = display;
        this.tooltip = tooltip;
    }

    public String getDisplay() {
        return display;
    }

    public String getTooltip() {
        return tooltip;
    }

    /**
     * Describes the connection of a node from its launcher.
     *
     * @param launcher the launcher of the node, or {@code null} if it has none
     * @param controller whether the node is the built-in node of the controller
     */
    static ConnectionInfo of(ComputerLauncher launcher, boolean controller) {
        if (controller) {
            return new ConnectionInfo("Built-in", "The built-in node runs inside the controller");
        }
        while (launcher instanceof DelegatingComputerLauncher delegating && delegating.getLauncher() != null) {
            launcher = delegating.getLauncher();
        }
        if (launcher == null) {
            return new ConnectionInfo("N/A", "");
        }
        if (launcher instanceof JNLPLauncher jnlp) {
            return jnlp.isWebSocket()
                    ? new ConnectionInfo("WebSocket", "Inbound agent connecting over WebSocket")
                    : new ConnectionInfo("JNLP", "Inbound agent connecting over the TCP agent port");
        }
        String className = launcher.getClass().getSimpleName();
        String lower = className.toLowerCase(Locale.ROOT);
        if (lower.contains("ssh")) {
            return new ConnectionInfo("SSH", "Outbound agent started by the controller over SSH");
        }
        if (lower.contains("command")) {
            return new ConnectionInfo("Command", "Outbound agent started by a command on the controller");
        }
        return new ConnectionInfo(
                className, "Launch method: " + launcher.getClass().getName());
    }

    @Override
    public String toString() {
        return display;
    }
}
