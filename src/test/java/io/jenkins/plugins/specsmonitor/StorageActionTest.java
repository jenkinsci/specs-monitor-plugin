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
class StorageActionTest {

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
        StorageAction action = j.jenkins.toComputer().getAction(StorageAction.class);

        List<StorageVolumes.Volume> volumes = action.getVolumes();

        assertNotNull(volumes);
        assertTrue(volumes.stream().anyMatch(v -> v.isWorkspace()));
    }

    @Test
    void pageRenders(JenkinsRule j) throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage page = wc.goTo("computer/(built-in)/storage/");

            assertEquals("Storage", page.getElementsByTagName("h1").get(0).getTextContent());
        }
    }
}
