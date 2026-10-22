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
    private final String ip;
    private final RemoteTarget.Family family;

    public ConnectionInfo(String display, String tooltip) {
        this(display, tooltip, null, null);
    }

    private ConnectionInfo(String display, String tooltip, String ip, RemoteTarget.Family family) {
        this.display = display;
        this.tooltip = tooltip;
        this.ip = ip;
        this.family = family;
    }

    /** The connection type only, e.g. {@code WebSocket}. */
    public String getDisplay() {
        return display;
    }

    /** IP address of the node, or {@code null} if unknown. */
    public String getIp() {
        return ip;
    }

    /** The connection type followed by the IP address if known, e.g. {@code WebSocket (10.0.0.5)}. */
    public String getLabel() {
        return ip == null ? display : display + " (" + ip + ")";
    }

    public String getTooltip() {
        if (ip == null) {
            return tooltip;
        }
        return tooltip.isEmpty() ? "IP address: " + ip : tooltip + ". IP address: " + ip;
    }

    /**
     * The remote access target as last reported by the node, or {@code null} if the node never
     * reported its address. It stays available while the node is offline.
     */
    RemoteTarget getRemoteTarget() {
        return ip == null ? null : new RemoteTarget(family, ip);
    }

    /** Returns a copy of this connection that also reports the given IP address and system. */
    ConnectionInfo withHost(String ip, RemoteTarget.Family family) {
        return new ConnectionInfo(display, tooltip, ip, family);
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
        return getLabel();
    }
}
