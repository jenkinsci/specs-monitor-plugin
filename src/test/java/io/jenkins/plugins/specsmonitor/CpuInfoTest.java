package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CpuInfoTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(
            delimiter = '|',
            value = {
                "Intel(R) Core(TM) i7-13700K|i7-13700K",
                "13th Gen Intel(R) Core(TM) i9-13900H|i9-13900H",
                "Intel(R) Core(TM) i7-8700 CPU @ 3.20GHz|i7-8700",
                "AMD Ryzen 7 5800X 8-Core Processor|Ryzen 7 5800X",
                "AMD Ryzen 9 7950X 16-Core Processor|Ryzen 9 7950X",
                "AMD EPYC 7763 64-Core Processor|EPYC 7763",
                "AMD   Ryzen   5   5600X|Ryzen 5 5600X",
                "intel(r) core(tm) i5-12400|i5-12400",
                "AMD FX(tm)-8320 Eight-Core Processor|FX-8320",
                "AMD FX(tm)-8120 Eight-Core Processor|FX-8120",
                "AMD Phenom(tm) II X4 955 Processor|Phenom II X4 955",
                "AMD Athlon(tm) II X2 250 Processor|Athlon II X2 250",
                "Intel(R) Core(TM)2 Duo CPU E8400 @ 3.00GHz|Core2 Duo E8400",
                "Intel(R) Xeon(R) CPU E5-2680 v4 @ 2.40GHz|Xeon E5-2680 v4",
                "AMD Opteron(tm) Processor 6174 Twelve-Core|Opteron 6174"
            })
    void shortNameStripsNoise(String raw, String expected) {
        assertEquals(expected, new CpuInfo(raw, 8).getShortName());
    }

    @Test
    void shortNameFallsBackToRawNameWhenEverythingIsNoise() {
        assertEquals("Intel CPU", new CpuInfo("Intel CPU", 4).getShortName());
    }

    @Test
    void shortNameFallsBackToRawNameWhenEmpty() {
        assertEquals("", new CpuInfo("", 4).getShortName());
    }

    @Test
    void nullNameDoesNotThrow() {
        CpuInfo info = new CpuInfo(null, 2);
        assertNull(info.getName());
        assertNull(info.getShortName());
    }

    @Test
    void getNameReturnsRawName() {
        assertEquals("Intel(R) Core(TM) i7-13700K", new CpuInfo("Intel(R) Core(TM) i7-13700K", 8).getName());
    }

    @Test
    void getThreadsReturnsThreadCount() {
        assertEquals(16, new CpuInfo("AMD Ryzen 9 7950X", 16).getThreads());
    }

    @Test
    void toStringUsesShortNameAndThreads() {
        assertEquals("i7-13700K (8 threads)", new CpuInfo("Intel(R) Core(TM) i7-13700K", 8).toString());
    }

    /**
     * CpuInfo is sent from agent to controller over remoting, so it must survive
     * serialization.
     */
    @Test
    void survivesSerializationRoundTrip() throws Exception {
        CpuInfo original = new CpuInfo("AMD Ryzen 7 5800X 8-Core Processor", 16);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        CpuInfo copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (CpuInfo) in.readObject();
        }

        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getThreads(), copy.getThreads());
        assertEquals(original.getShortName(), copy.getShortName());
    }
}
