package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CpuUsageInfoTest {

    @Test
    void displaysRoundedSystemPercent() {
        assertEquals("37%", new CpuUsageInfo(37.4, 2).getDisplay());
        assertEquals("100%", new CpuUsageInfo(99.6, 2).getDisplay());
        assertEquals("0%", new CpuUsageInfo(0, 0).getDisplay());
    }

    @Test
    void tooltipShowsSystemAndJvm() {
        assertEquals("System: 37% • JVM: 2%", new CpuUsageInfo(37, 2.2).getTooltip());
    }

    @Test
    void tooltipLeavesOutUnknownJvm() {
        assertEquals("System: 5%", new CpuUsageInfo(5, -1).getTooltip());
    }

    @Test
    void unknownIsNotAvailable() {
        CpuUsageInfo info = new CpuUsageInfo(-1, -1);
        assertEquals("N/A", info.getDisplay());
        assertEquals("", info.getTooltip());
    }
}
