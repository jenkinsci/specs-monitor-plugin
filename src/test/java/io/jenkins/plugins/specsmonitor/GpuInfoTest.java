package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GpuInfoTest {

    @Test
    void parsesNvidiaSmiNames() {
        assertEquals(
                "NVIDIA GeForce RTX 4090, NVIDIA A100",
                GpuInfo.fromNvidiaSmi("NVIDIA GeForce RTX 4090\nNVIDIA A100\n").getDisplay());
    }

    @Test
    void parsesLinuxDisplayControllers() {
        assertEquals(
                "NVIDIA Corporation AD102 [GeForce RTX 4090], Advanced Micro Devices [AMD/ATI] Navi 21",
                GpuInfo.fromLinuxLspci(
                                "01:00.0 VGA compatible controller: NVIDIA Corporation AD102 [GeForce RTX 4090] (rev a1)\n"
                                        + "02:00.0 3D controller: Advanced Micro Devices [AMD/ATI] Navi 21 (rev c1)\n"
                                        + "03:00.0 Audio device: NVIDIA Corporation Device 1234\n")
                        .getDisplay());
    }

    @Test
    void parsesWindowsAndMacNames() {
        assertEquals(
                "NVIDIA GeForce RTX 4090",
                GpuInfo.fromWindows("NVIDIA GeForce RTX 4090\n").getName());
        assertEquals(
                "Apple M2, AMD Radeon Pro 5500M",
                GpuInfo.fromMacSystemProfiler(
                                "      Chipset Model: Apple M2\n      Chipset Model: AMD Radeon Pro 5500M\n")
                        .getName());
    }

    @Test
    void reportsUnavailableForEmptyOutput() {
        assertEquals("N/A", GpuInfo.fromNvidiaSmi("").getDisplay());
        assertEquals(
                "N/A",
                GpuInfo.fromNvidiaSmi("NVIDIA-SMI has failed to communicate with the driver")
                        .getDisplay());
        assertEquals("N/A", new GpuInfo(null).getDisplay());
    }
}
