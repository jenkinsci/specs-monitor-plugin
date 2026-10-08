package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    void parsesBytesList() {
        assertArrayEquals(
                new long[] {17_179_869_184L, 8_589_934_592L, 9_663_676_416L},
                MemoryParser.fromBytesList("17179869184 8589934592 9663676416", 3));
    }

    @Test
    void bytesListWithMissingOrInvalidValues() {
        assertArrayEquals(
                new long[] {17_179_869_184L, 8_589_934_592L, -1},
                MemoryParser.fromBytesList("17179869184 8589934592", 3));
        assertArrayEquals(new long[] {-1, -1, -1}, MemoryParser.fromBytesList("a b c", 3));
        assertArrayEquals(new long[] {-1, -1, -1}, MemoryParser.fromBytesList("", 3));
        assertArrayEquals(new long[] {-1, -1, -1}, MemoryParser.fromBytesList(null, 3));
    }

    @Test
    void parsesMeminfoFreeAndAvailable() {
        List<String> lines =
                List.of("MemTotal:       16318356 kB", "MemFree:         1234567 kB", "MemAvailable:   12345678 kB");
        assertEquals(1_234_567L * 1024, MemoryParser.fromMeminfoKey(lines, "MemFree"));
        assertEquals(12_345_678L * 1024, MemoryParser.fromMeminfoKey(lines, "MemAvailable"));
        assertEquals(-1, MemoryParser.fromMeminfoKey(lines, "Missing"));
    }

    @Test
    void oldKernelWithoutMemAvailable() {
        List<String> lines = List.of("MemTotal: 16318356 kB", "MemFree: 1234567 kB");
        assertEquals(-1, MemoryParser.fromMeminfoKey(lines, "MemAvailable"));
    }

    @Test
    void parsesVmStat() {
        String output = """
                Mach Virtual Memory Statistics: (page size of 16384 bytes)
                Pages free:                               12345.
                Pages active:                            200000.
                Pages inactive:                           50000.
                Pages speculative:                         1000.
                """;
        long[] result = MemoryParser.fromVmStat(output);
        assertEquals(12_345L * 16_384, result[0]);
        assertEquals((12_345L + 50_000 + 1_000) * 16_384, result[1]);
    }

    @Test
    void vmStatFailureReturnsUnknown() {
        assertArrayEquals(new long[] {-1, -1}, MemoryParser.fromVmStat(""));
        assertArrayEquals(new long[] {-1, -1}, MemoryParser.fromVmStat(null));
    }
}
