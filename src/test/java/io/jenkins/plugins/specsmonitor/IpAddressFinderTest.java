package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class IpAddressFinderTest {

    private static InetAddress ip(String literal) throws Exception {
        return InetAddress.getByName(literal);
    }

    @Test
    void prefersPrivateIpv4() throws Exception {
        InetAddress best = IpAddressFinder.pickBest(List.of(ip("8.8.4.4"), ip("2001:db8::1"), ip("192.168.1.20")));

        assertEquals("192.168.1.20", best.getHostAddress());
    }

    @Test
    void fallsBackToPublicIpv4ThenIpv6() throws Exception {
        assertEquals(
                "8.8.4.4",
                IpAddressFinder.pickBest(List.of(ip("2001:db8::1"), ip("8.8.4.4")))
                        .getHostAddress());
        assertEquals(
                "2001:db8:0:0:0:0:0:1",
                IpAddressFinder.pickBest(List.of(ip("2001:db8::1"))).getHostAddress());
    }

    @Test
    void ignoresLoopbackAndLinkLocal() throws Exception {
        assertNull(IpAddressFinder.pickBest(List.of(ip("127.0.0.1"), ip("169.254.1.1"), ip("fe80::1"), ip("::1"))));
    }

    @Test
    void emptyListHasNoAddress() {
        assertNull(IpAddressFinder.pickBest(List.of()));
    }
}
