package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

/** Operating system family and IP address of a node, used to offer remote access to it. */
public class RemoteTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Only characters of IPv4 and IPv6 literals, so the value is safe in links and files. */
    private static final Pattern IP_LITERAL = Pattern.compile("[0-9a-fA-F:.]{2,45}");

    /** Operating system family of a node. */
    public enum Family {
        WINDOWS,
        LINUX,
        MAC,
        OTHER;

        static Family fromOsName(String osName) {
            String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
            if (os.startsWith("windows")) {
                return WINDOWS;
            }
            if (os.startsWith("linux")) {
                return LINUX;
            }
            if (os.startsWith("mac")) {
                return MAC;
            }
            return OTHER;
        }
    }

    private final Family family;
    private final String ip;

    /**
     * @param ip IP address of the node; it is dropped if it is not an IP literal
     *           because it comes from the node, which may not be trusted
     */
    public RemoteTarget(Family family, String ip) {
        this.family = family == null ? Family.OTHER : family;
        this.ip = ip != null && IP_LITERAL.matcher(ip).matches() ? ip : null;
    }

    public Family getFamily() {
        return family;
    }

    /** IP address of the node, or {@code null} if unknown. */
    public String getIp() {
        return ip;
    }

    /** Whether the node can be reached with a Remote Desktop client. */
    public boolean isWindows() {
        return ip != null && family == Family.WINDOWS;
    }

    /** Whether the node can be reached with an SSH client. */
    public boolean isSshCapable() {
        return ip != null && (family == Family.LINUX || family == Family.MAC);
    }

    /** The address as used in a URI, with brackets around IPv6 literals. */
    public String getUriHost() {
        return ip != null && ip.indexOf(':') >= 0 ? "[" + ip + "]" : ip;
    }

    /** Link that opens the SSH client registered with the browser's operating system. */
    public String getSshUri() {
        return ip == null ? null : "ssh://" + getUriHost();
    }

    /**
     * Link that opens the Remote Desktop client registered for {@code rdp://}, such as
     * the Microsoft Remote Desktop and Windows App clients. The built-in Windows client
     * does not register it.
     */
    public String getRdpUri() {
        return ip == null ? null : "rdp://full%20address=s:" + ip;
    }

    /** Command to paste into a terminal. */
    public String getSshCommand() {
        return ip == null ? null : "ssh " + ip;
    }

    /** Contents of an {@code .rdp} file that opens Remote Desktop Connection for this node. */
    String toRdpFile() {
        return "full address:s:" + ip + "\r\nprompt for credentials:i:1\r\n";
    }
}
