package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryInfoTest {

    @Test
    void formatsGigabytes() {
        assertEquals("16 GiB", new MemoryInfo(17_179_869_184L).getDisplay());
        assertEquals("15.6 GiB", new MemoryInfo(16_318_356L * 1024).getDisplay());
    }

    @Test
    void formatsMegabytes() {
        assertEquals("512 MiB", new MemoryInfo(536_870_912L).getDisplay());
    }

    @Test
    void formatsTerabytes() {
        assertEquals("1.5 TiB", new MemoryInfo(1_649_267_441_664L).getDisplay());
    }

    @Test
    void unknownIsNotAvailable() {
        assertEquals("N/A", new MemoryInfo(0).getDisplay());
        assertEquals("N/A", new MemoryInfo(-1).getDisplay());
    }

    @Test
    void toStringIsDisplay() {
        assertEquals("16 GiB", new MemoryInfo(17_179_869_184L).toString());
    }
}
