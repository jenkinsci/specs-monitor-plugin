package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GpuInfoTest {

    @Test
    void selectsFirstNvidiaSmiResult() {
        assertEquals(
                "RTX 4090",
                GpuInfo.fromNvidiaSmi("NVIDIA GeForce RTX 4090\nNVIDIA A100\n").getDisplay());
    }

    @Test
    void parsesLinuxDisplayControllers() {
        assertEquals(
                "RTX 4090",
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
                "Arc A770",
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

    @Test
    void shortensCommonVendorNamesAndKeepsFullTooltip() {
        assertShortName("NVIDIA GeForce RTX 4060 Laptop GPU", "RTX 4060 Laptop");
        assertShortName("NVIDIA GeForce RTX 5060", "RTX 5060");
        assertShortName("NVIDIA GeForce RTX 5070 Ti", "RTX 5070 Ti");
        assertShortName("NVIDIA GeForce RTX 2080 SUPER", "RTX 2080 SUPER");
        assertShortName("NVIDIA GeForce GTX 1660", "GTX 1660");
        assertShortName("NVIDIA GeForce GTX 1660 Ti", "GTX 1660 Ti");
        assertShortName("NVIDIA GeForce GTX 1660 SUPER", "GTX 1660 SUPER");
        assertShortName("NVIDIA GeForce GTX 980M", "GTX 980M");
        assertShortName("NVIDIA Quadro P4000", "Quadro P4000");
        assertShortName("NVIDIA Quadro RTX 6000", "Quadro RTX 6000");
        assertShortName("NVIDIA RTX A6000", "RTX A6000");
        assertShortName("NVIDIA GeForce RTX 2070 with Max-Q Design", "RTX 2070 Max-Q");
        assertShortName("NVIDIA Tesla P100-PCIE-16GB", "Tesla P100");
        assertShortName("NVIDIA Tesla V100-SXM2-16GB", "Tesla V100");
        assertShortName("NVIDIA L40S", "L40S");
        assertShortName("NVIDIA B200", "B200");
        assertShortName("NVIDIA GB200", "GB200");
        assertShortName("Intel(R) Arc(TM) A580 Graphics", "Arc A580");
        assertShortName("Intel(R) Arc(TM) B570 Graphics", "Arc B570");
        assertShortName("Intel(R) Arc(TM) Pro A60 Graphics", "Arc Pro A60");
        assertShortName("Intel(R) Arc(TM) Pro B60 Graphics", "Arc Pro B60");
        assertShortName("Intel(R) Arc(TM) A370M Graphics", "Arc A370M");
        assertShortName("Intel(R) Arc(TM) 130V", "Arc 130V");
        assertShortName("Intel(R) Data Center GPU Max 1100", "Max 1100");
        assertShortName("Intel(R) Data Center GPU Flex 140", "Flex 140");
        assertShortName("AMD Radeon RX 9060 XT", "RX 9060 XT");
        assertShortName("AMD Radeon RX 9050", "RX 9050");
        assertShortName("AMD Radeon RX 7900 XTX", "RX 7900 XTX");
        assertShortName("AMD Radeon RX 7900M", "RX 7900M");
        assertShortName("AMD Radeon (TM) Pro WX 9100", "Pro WX 9100");
        assertShortName("AMD Radeon AI PRO R9700", "AI PRO R9700");
        assertShortName("Radeon RX Vega M GL Graphics", "RX Vega M GL");
        assertShortName("AMD Radeon RX 7600M XT", "RX 7600M XT");
        assertShortName("AMD Radeon RX 7700S", "RX 7700S");
        assertShortName("AMD Instinct MI100 Accelerator", "Instinct MI100");
        assertShortName("AMD Instinct MI300X Accelerator", "Instinct MI300X");
        assertShortName("AMD Instinct MI300A Accelerator", "Instinct MI300A");

        GpuInfo info = new GpuInfo("NVIDIA GeForce RTX 4060 Laptop GPU");
        assertEquals("NVIDIA GeForce RTX 4060 Laptop GPU", info.getName());
        assertEquals("NVIDIA GeForce RTX 4060 Laptop GPU • Score: 2,923.74", info.getTooltip());
    }

    @Test
    void scoreMatchesFullNameBeforeShortName() {
        assertEquals("11,553.09", new GpuInfo("NVIDIA GeForce RTX 4090").getScore());
        assertEquals("17,418.97", new GpuInfo("NVIDIA GeForce RTX 5090 D").getScore());
        assertEquals("11,553.09", new GpuInfo("NVIDIA Corporation AD102 [GeForce RTX 4090]").getScore());
        assertNull(new GpuInfo("Some Unknown GPU").getScore());
        assertEquals("Some Unknown GPU", new GpuInfo("Some Unknown GPU").getTooltip());
    }

    private static void assertShortName(String fullName, String expected) {
        assertEquals(expected, new GpuInfo(fullName).getShortName());
    }
}
