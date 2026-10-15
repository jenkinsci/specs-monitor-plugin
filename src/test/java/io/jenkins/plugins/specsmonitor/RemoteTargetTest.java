package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.specsmonitor.RemoteTarget.Family;
import org.junit.jupiter.api.Test;

class RemoteTargetTest {

    @Test
    void detectsFamilyFromOsName() {
        assertEquals(Family.WINDOWS, Family.fromOsName("Windows 11"));
        assertEquals(Family.LINUX, Family.fromOsName("Linux"));
        assertEquals(Family.MAC, Family.fromOsName("Mac OS X"));
        assertEquals(Family.OTHER, Family.fromOsName("FreeBSD"));
        assertEquals(Family.OTHER, Family.fromOsName(null));
    }

    @Test
    void windowsUsesRemoteDesktop() {
        RemoteTarget target = new RemoteTarget(Family.WINDOWS, "192.168.1.20");

        assertTrue(target.isWindows());
        assertFalse(target.isSshCapable());
        assertEquals("full address:s:192.168.1.20\r\nprompt for credentials:i:1\r\n", target.toRdpFile());
    }

    @Test
    void linuxAndMacUseSsh() {
        for (Family family : new Family[] {Family.LINUX, Family.MAC}) {
            RemoteTarget target = new RemoteTarget(family, "10.0.0.5");

            assertTrue(target.isSshCapable());
            assertFalse(target.isWindows());
            assertEquals("ssh://10.0.0.5", target.getSshUri());
            assertEquals("ssh 10.0.0.5", target.getSshCommand());
        }
    }

    @Test
    void ipv6IsBracketedInUri() {
        assertEquals("ssh://[2001:db8::1]", new RemoteTarget(Family.LINUX, "2001:db8::1").getSshUri());
    }

    @Test
    void otherSystemsHaveNoRemoteAccess() {
        RemoteTarget target = new RemoteTarget(Family.OTHER, "10.0.0.5");

        assertFalse(target.isWindows());
        assertFalse(target.isSshCapable());
    }

    @Test
    void rejectsAddressesThatAreNotIpLiterals() {
        assertNull(new RemoteTarget(Family.WINDOWS, "host\r\nfull address:s:evil").getIp());
        assertNull(new RemoteTarget(Family.LINUX, "\"><script>").getIp());
        assertNull(new RemoteTarget(Family.LINUX, null).getIp());
        assertFalse(new RemoteTarget(Family.WINDOWS, "evil.example").isWindows());
    }
}
