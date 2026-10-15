package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UptimeInfoTest {

    @Test
    void formatsDaysHoursMinutes() {
        assertEquals("3d 4h", new UptimeInfo(3 * 86400 + 4 * 3600 + 12 * 60).getDisplay());
        assertEquals("5h 12m", new UptimeInfo(5 * 3600 + 12 * 60).getDisplay());
        assertEquals("7m", new UptimeInfo(7 * 60 + 30).getDisplay());
        assertEquals("0m", new UptimeInfo(10).getDisplay());
    }

    @Test
    void tooltipSpellsOutDuration() {
        assertEquals("Up for 3 days, 4 hours, 12 minutes", new UptimeInfo(3 * 86400 + 4 * 3600 + 12 * 60).getTooltip());
        assertEquals("Up for 1 day, 1 hour, 1 minute", new UptimeInfo(86400 + 3600 + 60).getTooltip());
    }

    @Test
    void unknownIsNotAvailable() {
        assertEquals("N/A", new UptimeInfo(-1).getDisplay());
        assertEquals("", new UptimeInfo(-1).getTooltip());
    }
}
