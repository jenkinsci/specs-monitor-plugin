package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryInfoTest {

    @Test
    void formatsGigabytes() {
        assertEquals("16 GiB", new MemoryInfo(-1, -1, 17_179_869_184L).getDisplay());
        assertEquals("15.6 GiB", new MemoryInfo(-1, -1, 16_318_356L * 1024).getDisplay());
    }

    @Test
    void formatsMegabytes() {
        assertEquals("512 MiB", new MemoryInfo(-1, -1, 536_870_912L).getDisplay());
    }

    @Test
    void formatsTerabytes() {
        assertEquals("1.5 TiB", new MemoryInfo(-1, -1, 1_649_267_441_664L).getDisplay());
    }

    @Test
    void unknownIsNotAvailable() {
        assertEquals("N/A", new MemoryInfo(0).getDisplay());
        assertEquals("N/A", new MemoryInfo(-1).getDisplay());
    }

    @Test
    void displayPrefersAvailableOverFree() {
        assertEquals("8 GiB", new MemoryInfo(17_179_869_184L, 1_073_741_824L, 8_589_934_592L).getDisplay());
    }

    @Test
    void displayFallsBackToFree() {
        assertEquals("1 GiB", new MemoryInfo(17_179_869_184L, 1_073_741_824L, -1).getDisplay());
    }

    @Test
    void displayIsNotAvailableWhenOnlyTotalIsKnown() {
        assertEquals("N/A", new MemoryInfo(17_179_869_184L).getDisplay());
    }

    @Test
    void toStringIsDisplay() {
        assertEquals("16 GiB", new MemoryInfo(-1, -1, 17_179_869_184L).toString());
    }

    @Test
    void tooltipShowsFreeOutOfTotalWhenFreeAndAvailableMatch() {
        MemoryInfo info = new MemoryInfo(68397354188L, 46707769344L, 46707769344L);
        assertEquals("43.5 GiB free of 63.7 GiB", info.getTooltip());
    }

    @Test
    void tooltipShowsAvailableAndFreeWhenTheyDiffer() {
        MemoryInfo info = new MemoryInfo(17_179_869_184L, 1_288_490_189L, 9_019_431_321L);
        assertEquals("Available: 8.4 GiB of 16 GiB, Free: 1.2 GiB", info.getTooltip());
    }

    @Test
    void tooltipWithOnlyFreeKnown() {
        assertEquals("0 MiB free of 16 GiB", new MemoryInfo(17_179_869_184L, 0, -1).getTooltip());
    }

    @Test
    void tooltipWithOnlyAvailableKnown() {
        assertEquals("8 GiB free of 16 GiB", new MemoryInfo(17_179_869_184L, -1, 8_589_934_592L).getTooltip());
    }

    @Test
    void tooltipWithoutTotal() {
        assertEquals("8 GiB free", new MemoryInfo(-1, 8_589_934_592L, 8_589_934_592L).getTooltip());
    }

    @Test
    void tooltipIsEmptyWhenUnknown() {
        assertEquals("", new MemoryInfo(17_179_869_184L).getTooltip());
    }
}
