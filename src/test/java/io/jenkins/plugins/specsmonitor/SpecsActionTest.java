package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SpecsActionTest {

    @Test
    void mountPointIsTakenFromFileStoreDescription() {
        assertEquals("/mnt/data", StorageVolumes.mountPoint("/mnt/data (/dev/sdb1)"));
        assertEquals("/", StorageVolumes.mountPoint("/ (overlay)"));
        assertEquals("/mnt/my disk", StorageVolumes.mountPoint("/mnt/my disk (/dev/sdc1)"));
        assertEquals("/data", StorageVolumes.mountPoint("/data"));
    }

    @Test
    void pseudoFilesystemsAreRecognized() {
        assertTrue(StorageVolumes.isPseudoFilesystem("proc"));
        assertTrue(StorageVolumes.isPseudoFilesystem("TMPFS"));
        assertTrue(StorageVolumes.isPseudoFilesystem(null));
        assertFalse(StorageVolumes.isPseudoFilesystem("ext4"));
        assertFalse(StorageVolumes.isPseudoFilesystem("overlay"));
        assertFalse(StorageVolumes.isPseudoFilesystem("NTFS"));
    }

    @Test
    void powerShellDriveTypesAreParsed() {
        Map<String, String> types =
                DriveTypes.fromPowerShell("C|SSD|NVMe\nd|HDD|SATA\nE|Unspecified|Virtual\nF|SSD|SATA\n");

        assertEquals("SSD (NVMe)", types.get("C"));
        assertEquals("HDD", types.get("D"));
        assertEquals("SSD", types.get("F"));
        assertFalse(types.containsKey("E"));
        assertTrue(DriveTypes.fromPowerShell("").isEmpty());
        assertTrue(DriveTypes.fromPowerShell(null).isEmpty());
    }

    @Test
    void windowsScriptSurvivesCommandLineQuoting() {
        assertFalse(DriveTypes.WINDOWS_SCRIPT.contains("\""));
    }

    @Test
    void rotationalFlagDecidesBetweenSsdAndHdd() {
        assertEquals("HDD", DriveTypes.fromRotational("1\n", false));
        assertEquals("SSD", DriveTypes.fromRotational("0\n", false));
        assertEquals("SSD (NVMe)", DriveTypes.fromRotational("0", true));
        assertNull(DriveTypes.fromRotational("", false));
        assertNull(DriveTypes.fromRotational(null, false));
    }

    @Test
    void diskutilSolidStateIsParsed() {
        assertEquals("SSD", DriveTypes.fromDiskutil("   Device Node:  /dev/disk3s1\n   Solid State:  Yes\n"));
        assertEquals("HDD", DriveTypes.fromDiskutil("   Solid State:              No"));
        assertNull(DriveTypes.fromDiskutil("   Device Node:  /dev/disk3s1\n"));
    }

    @Test
    void nonBlockDevicesHaveNoDriveType() {
        assertNull(DriveTypes.forLinux("overlay"));
        assertNull(DriveTypes.forLinux("//nas/share"));
        assertNull(DriveTypes.forLinux(null));
        assertNull(DriveTypes.forMac("/Volumes/share", "//user@nas/share"));
        assertNull(DriveTypes.forMac("/", null));
    }

    @Test
    void volumesOfBuiltInNodeContainTheWorkspace(JenkinsRule j) {
        SpecsAction action = j.jenkins.toComputer().getAction(SpecsAction.class);

        List<StorageVolumes.Volume> volumes = action.getVolumes();

        assertNotNull(volumes);
        assertTrue(volumes.stream().anyMatch(v -> v.isWorkspace()));
    }

    @Test
    void ramOfBuiltInNodeIsReported(JenkinsRule j) {
        SpecsAction action = j.jenkins.toComputer().getAction(SpecsAction.class);

        assertNotNull(action.getRam());
    }

    @Test
    void powerShellMemoryModulesAreParsed() {
        List<RamSpecs.Module> modules =
                RamSpecs.fromPowerShell("17179869184|3200|3200|26\n8589934592||2667|34\n0|0|0|0\n");

        assertEquals(2, modules.size());
        assertEquals("16 GiB", modules.get(0).getSizeDisplay());
        assertEquals("DDR4", modules.get(0).getTypeDisplay());
        assertEquals("3200 MHz", modules.get(0).getSpeedDisplay());
        assertEquals("DDR5", modules.get(1).getTypeDisplay());
        assertEquals("2667 MHz", modules.get(1).getSpeedDisplay());
        assertTrue(RamSpecs.fromPowerShell(null).isEmpty());
    }

    @Test
    void dmidecodeMemoryModulesAreParsed() {
        String output = "Memory Device\n\tSize: 16 GB\n\tType: DDR4\n\tSpeed: 3200 MT/s\n"
                + "\tConfigured Memory Speed: 2933 MT/s\n\n"
                + "Memory Device\n\tSize: No Module Installed\n\tType: Unknown\n";

        List<RamSpecs.Module> modules = RamSpecs.fromKeyValues(output);

        assertEquals(1, modules.size());
        assertEquals("16 GiB", modules.get(0).getSizeDisplay());
        assertEquals("2933 MHz", modules.get(0).getSpeedDisplay());
    }

    @Test
    void systemProfilerMemoryIsParsed() {
        List<RamSpecs.Module> dimms = RamSpecs.fromKeyValues(
                "Memory:\n  BANK 0/DIMM0:\n    Size: 8 GB\n    Type: DDR4\n    Speed: 2667 MHz\n");
        List<RamSpecs.Module> soldered = RamSpecs.fromKeyValues("Memory:\n  Memory: 16 GB\n  Type: LPDDR5\n");

        assertEquals("2667 MHz", dimms.get(0).getSpeedDisplay());
        assertEquals(1, soldered.size());
        assertEquals("LPDDR5", soldered.get(0).getTypeDisplay());
        assertEquals("N/A", soldered.get(0).getSpeedDisplay());
    }

    @Test
    void windowsMemoryScriptSurvivesCommandLineQuoting() {
        assertFalse(RamSpecs.WINDOWS_SCRIPT.contains("\""));
    }

    @Test
    void pageRenders(JenkinsRule j) throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage page = wc.goTo("computer/(built-in)/specs/");

            assertEquals("Specs", page.getElementsByTagName("h1").get(0).getTextContent());
        }
    }
}
