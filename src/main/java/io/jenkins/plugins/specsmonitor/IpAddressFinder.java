package io.jenkins.plugins.specsmonitor;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Finds the IP address a node uses to reach the network. */
final class IpAddressFinder {

    private static final Logger LOGGER = Logger.getLogger(IpAddressFinder.class.getName());

    private IpAddressFinder() {}

    /** Returns the IP address of this machine, or {@code null} if none was found. */
    static String find() {
        InetAddress routed = routedAddress();
        if (isUsable(routed)) {
            return routed.getHostAddress();
        }
        InetAddress best = pickBest(interfaceAddresses());
        return best == null ? null : best.getHostAddress();
    }

    /**
     * The local address the operating system would use to reach the internet. UDP
     * "connect" only selects a route, so no packet is sent and this also avoids
     * virtual adapters such as Docker bridges.
     */
    private static InetAddress routedAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress("192.0.2.1", 9));
            return socket.getLocalAddress();
        } catch (SocketException | RuntimeException e) {
            LOGGER.log(Level.FINE, "No default route to determine the IP address", e);
            return null;
        }
    }

    private static List<InetAddress> interfaceAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nif.isUp() && !nif.isLoopback() && !nif.isVirtual()) {
                    addresses.addAll(Collections.list(nif.getInetAddresses()));
                }
            }
        } catch (SocketException e) {
            LOGGER.log(Level.FINE, "Could not list network interfaces", e);
        }
        return addresses;
    }

    /** Prefers private IPv4, then any usable IPv4, then any usable address. */
    static InetAddress pickBest(List<InetAddress> candidates) {
        InetAddress anyIpv4 = null;
        InetAddress any = null;
        for (InetAddress a : candidates) {
            if (!isUsable(a)) {
                continue;
            }
            if (a instanceof Inet4Address) {
                if (a.isSiteLocalAddress()) {
                    return a;
                }
                if (anyIpv4 == null) {
                    anyIpv4 = a;
                }
            } else if (any == null) {
                any = a;
            }
        }
        return anyIpv4 != null ? anyIpv4 : any;
    }

    private static boolean isUsable(InetAddress a) {
        return a != null && !a.isAnyLocalAddress() && !a.isLoopbackAddress() && !a.isLinkLocalAddress();
    }
}
