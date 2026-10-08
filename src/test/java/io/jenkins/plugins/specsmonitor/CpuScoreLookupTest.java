package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CpuScoreLookupTest {

    @Test
    void parsesAndFormatsScoresForDisplayedNames() throws Exception {
        Map<String, BigDecimal> scores =
                CpuScoreLookup.parseCsv(new StringReader("Device Name,Median Score,Number of Benchmarks\n"
                        + "\"POWER10 (architected), altivec supported\",3393.25,2\n"
                        + "Intel Core i7-13700K,3393,1\n"));

        assertEquals(new BigDecimal("3393.25"), scores.get("power10 (architected), altivec supported"));
        assertEquals("3,393.25", CpuScoreLookup.formatScore(scores.get("power10 (architected), altivec supported")));
        assertEquals("3,393", CpuScoreLookup.formatScore(scores.get("i7-13700k")));
    }

    @Test
    void ignoresMalformedRowsAndMissingNames() throws Exception {
        Map<String, BigDecimal> scores = CpuScoreLookup.parseCsv(new StringReader(
                "Device Name,Median Score,Number of Benchmarks\n" + "i7-13700K,not-a-score,1\n\"broken,3393,1\n"));

        assertNull(scores.get("i7-13700K"));
        assertNull(scores.get("missing CPU"));
    }

    @Test
    void scoreIsAbsentWhenNoBundledEntryMatches() {
        assertNull(new CpuInfo("CPU with no score row", 8).getScore());
    }

    @Test
    void bundledScoreMatchesShortCpuDisplayName() {
        assertEquals("2,950.43", new CpuInfo("AMD EPYC 9B45 128-Core Processor", 128).getScore());
    }

    @Test
    void deviceNamesAreShortenedBeforeLookup() throws Exception {
        Map<String, BigDecimal> scores = CpuScoreLookup.parseCsv(new StringReader(
                "Number of Benchmarks,Median Score,Device Name\n" + "1,662.86,AMD Ryzen 9 9950X 16-Core Processor\n"));

        assertEquals(new BigDecimal("662.86"), scores.get("ryzen 9 9950x"));
    }
}
