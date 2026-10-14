package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GpuInfoTest {

    @Test
    void selectsFirstNvidiaSmiResult() {
        assertEquals(
                "NVIDIA GeForce RTX 4090",
                GpuInfo.fromNvidiaSmi("NVIDIA GeForce RTX 4090\nNVIDIA A100\n").getDisplay());
    }

    @Test
    void parsesLinuxDisplayControllers() {
        assertEquals(
                "NVIDIA Corporation AD102 [GeForce RTX 4090]",
                GpuInfo.fromLinuxLspci(
                                "01:00.0 VGA compatible controller: NVIDIA Corporation AD102 [GeForce RTX 4090] (rev a1)\n"
                                        + "02:00.0 3D controller: Advanced Micro Devices [AMD/ATI] Navi 21 (rev c1)\n"
                                        + "03:00.0 Display controller: Intel Corporation DG2 [Arc A770] (rev 08)\n"
                                        + "04:00.0 Audio device: NVIDIA Corporation Device 1234\n")
                        .getDisplay());
    }

    @Test
    void selectsFirstLinuxController() {
        assertEquals(
                "Intel Corporation DG2 [Arc A770]",
                GpuInfo.fromLinuxLspci("00:02.0 VGA compatible controller: Intel Corporation DG2 [Arc A770] (rev 08)\n"
                                + "01:00.0 3D controller: Advanced Micro Devices [AMD/ATI] Navi 21 (rev c1)\n")
                        .getDisplay());
    }

    @Test
    void parsesWindowsAndMacNames() {
        assertEquals(
                "NVIDIA GeForce RTX 4090",
                GpuInfo.fromWindows("NVIDIA GeForce RTX 4090\n").getName());
        assertEquals(
                "Apple M2",
                GpuInfo.fromMacSystemProfiler(
                                "      Chipset Model: Apple M2\n      Chipset Model: AMD Radeon Pro 5500M\n")
                        .getName());
    }

    @Test
    void prefersDedicatedPciGpuOverVirtualDisplayDriver() {
        assertEquals(
                "NVIDIA GeForce RTX 4090",
                GpuInfo.fromWindows("Virtual Display Driver\tROOT\\DISPLAY\\0000\n"
                                + "NVIDIA GeForce RTX 4090\tPCI\\VEN_10DE&DEV_2684\n"
                                + "Intel UHD Graphics\tPCI\\VEN_8086&DEV_46A6\n")
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
