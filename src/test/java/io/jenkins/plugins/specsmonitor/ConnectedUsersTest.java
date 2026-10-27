package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConnectedUsersTest {

    @Test
    void parsesQueryUser() {
        String output = " USERNAME              SESSIONNAME        ID  STATE   IDLE TIME  LOGON TIME\n"
                + ">jdoe                  rdp-tcp#0           2  Active          .  10/27/2026 9:00 AM\n"
                + " build                                     3  Disc         1:02  10/26/2026 8:00 AM\n";
        List<ConnectedUsers.Session> sessions = ConnectedUsers.fromQueryUser(output);
        assertEquals(2, sessions.size());
        assertEquals("jdoe", sessions.get(0).getUser());
        assertEquals("rdp-tcp#0, Active", sessions.get(0).getDetail());
        assertEquals("build", sessions.get(1).getUser());
        assertEquals("Disc", sessions.get(1).getDetail());
    }

    @Test
    void parsesWhoAndMergesRepeatedUsers() {
        String output = "jdoe pts/0 2026-10-27 09:00 (10.0.0.5)\n" + "jdoe pts/1 2026-10-27 09:30\n"
                + "ci tty1 2026-10-26 08:00\n";
        List<ConnectedUsers.Session> sessions = ConnectedUsers.fromWho(output);
        assertEquals(2, sessions.size());
        assertEquals("jdoe", sessions.get(0).getUser());
        assertEquals(
                "pts/0, since 2026-10-27 09:00 (10.0.0.5); pts/1, since 2026-10-27 09:30",
                sessions.get(0).getDetail());
    }

    @Test
    void emptyOrInvalidOutputHasNoUsers() {
        assertTrue(ConnectedUsers.fromQueryUser("").isEmpty());
        assertTrue(ConnectedUsers.fromQueryUser(null).isEmpty());
        assertTrue(ConnectedUsers.fromQueryUser("No User exists for *").isEmpty());
        assertTrue(ConnectedUsers.fromWho("").isEmpty());
        assertTrue(ConnectedUsers.fromWho(null).isEmpty());
    }
}
