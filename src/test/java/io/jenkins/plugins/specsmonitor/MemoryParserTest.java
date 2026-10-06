package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryParserTest {

    @Test
    void parsesBytesFromSysctlOrPowerShell() {
        assertEquals(17_179_869_184L, MemoryParser.fromBytes("17179869184"));
        assertEquals(17_179_869_184L, MemoryParser.fromBytes("17179869184\r\n"));
    }

    @Test
    void invalidBytesReturnMinusOne() {
        assertEquals(-1, MemoryParser.fromBytes(""));
        assertEquals(-1, MemoryParser.fromBytes(null));
        assertEquals(-1, MemoryParser.fromBytes("not a number"));
        assertEquals(-1, MemoryParser.fromBytes("0"));
        assertEquals(-1, MemoryParser.fromBytes("-5"));
    }

    @Test
    void parsesMeminfo() {
        List<String> lines =
                List.of("MemTotal:       16318356 kB", "MemFree:         1234567 kB", "MemAvailable:   12345678 kB");
        assertEquals(16_318_356L * 1024, MemoryParser.fromMeminfo(lines));
    }

    @Test
    void meminfoWithoutMemTotalReturnsMinusOne() {
        assertEquals(-1, MemoryParser.fromMeminfo(List.of("MemFree: 1234 kB")));
        assertEquals(-1, MemoryParser.fromMeminfo(List.of()));
    }

    @Test
    void meminfoWithGarbageReturnsMinusOne() {
        assertEquals(-1, MemoryParser.fromMeminfo(List.of("MemTotal: lots kB")));
    }
}
