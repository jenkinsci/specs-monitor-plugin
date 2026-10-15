package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UptimeParserTest {

    @Test
    void parsesProcUptime() {
        assertEquals(12345, UptimeParser.fromSeconds("12345.67 98765.43\n"));
    }

    @Test
    void parsesWholeSeconds() {
        assertEquals(3600, UptimeParser.fromSeconds("3600"));
    }

    @Test
    void invalidSecondsAreUnknown() {
        assertEquals(-1, UptimeParser.fromSeconds(""));
        assertEquals(-1, UptimeParser.fromSeconds(null));
        assertEquals(-1, UptimeParser.fromSeconds("abc"));
        assertEquals(-1, UptimeParser.fromSeconds("-5"));
    }

    @Test
    void parsesMacBootTime() {
        assertEquals(
                500,
                UptimeParser.fromMacBootTime("{ sec = 1697000000, usec = 123 } Wed Oct 11 05:33:20 2023", 1697000500L));
    }

    @Test
    void invalidMacBootTimeIsUnknown() {
        assertEquals(-1, UptimeParser.fromMacBootTime("", 1697000500L));
        assertEquals(-1, UptimeParser.fromMacBootTime("{ sec = 1697001000, usec = 0 }", 1697000500L));
    }
}
