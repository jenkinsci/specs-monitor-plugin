package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionInfoTest {

    @Test
    void jvmShowsNumbersWithFullVersionAsTooltip() {
        VersionInfo info = VersionInfo.fromJvm(Runtime.Version.parse("25.0.4.1+1-LTS"));

        assertEquals("25.0.4.1", info.getDisplay());
        assertEquals("25.0.4.1+1-LTS", info.getTooltip());
    }

    @Test
    void jvmWithoutUpdateComponents() {
        VersionInfo info = VersionInfo.fromJvm(Runtime.Version.parse("21+35"));

        assertEquals("21", info.getDisplay());
        assertEquals("21+35", info.getTooltip());
    }

    @Test
    void remotingDropsHashSuffix() {
        VersionInfo info = VersionInfo.fromRemoting("3355.3357.v931d3c992987");

        assertEquals("3355.3357", info.getDisplay());
        assertEquals("3355.3357.v931d3c992987", info.getTooltip());
    }

    @Test
    void remotingWithoutHashIsShownAsIs() {
        VersionInfo info = VersionInfo.fromRemoting("3355.3357");

        assertEquals("3355.3357", info.getDisplay());
        assertEquals("3355.3357", info.getTooltip());
    }

    @Test
    void remotingKeepsUnrecognizedVersion() {
        assertEquals("unknown", VersionInfo.fromRemoting("unknown").getDisplay());
    }

    @Test
    void jvmFeatureComparisonIgnoresLaterComponents() {
        VersionInfo jvm17 = VersionInfo.fromJvm(Runtime.Version.parse("17.0.12+7"));
        VersionInfo jvm21 = VersionInfo.fromJvm(Runtime.Version.parse("21.0.1+12"));
        VersionInfo jvm21Newer = VersionInfo.fromJvm(Runtime.Version.parse("21.0.9+1"));

        assertTrue(jvm17.isFeatureOlderThan(jvm21));
        assertFalse(jvm21.isFeatureOlderThan(jvm17));
        assertFalse(jvm21.isFeatureOlderThan(jvm21Newer));
    }

    @Test
    void remotingComparesComponentWise() {
        VersionInfo old = VersionInfo.fromRemoting("3352.v17a_fb_4b_2773f");
        VersionInfo twoNumbers = VersionInfo.fromRemoting("3355.3357.v931d3c992987");

        assertTrue(old.isOlderThan(twoNumbers));
        assertFalse(twoNumbers.isOlderThan(old));
        assertFalse(twoNumbers.isOlderThan(VersionInfo.fromRemoting("3355.3357.vabcdef")));
        assertTrue(VersionInfo.fromRemoting("3355.v1").isOlderThan(twoNumbers));
    }

    @Test
    void gitShowsNumbersWithFullVersionAsTooltip() {
        VersionInfo info = VersionInfo.fromGit("git version 2.43.0.windows.1\n");

        assertEquals("2.43.0", info.getDisplay());
        assertEquals("2.43.0.windows.1", info.getTooltip());
    }

    @Test
    void gitKeepsVendorSuffixInTooltip() {
        VersionInfo info = VersionInfo.fromGit("git version 2.39.5 (Apple Git-154)");

        assertEquals("2.39.5", info.getDisplay());
        assertEquals("2.39.5 (Apple Git-154)", info.getTooltip());
    }

    @Test
    void gitOutputThatIsNotAVersionIsUnknown() {
        assertEquals("N/A", VersionInfo.fromGit("").getDisplay());
        assertEquals("N/A", VersionInfo.fromGit(null).getDisplay());
        assertEquals("N/A", VersionInfo.fromGit("'git' is not recognized").getDisplay());
    }

    @Test
    void gitComparesComponentWise() {
        VersionInfo old = VersionInfo.fromGit("git version 2.39.5");
        VersionInfo current = VersionInfo.fromGit("git version 2.43.0.windows.1");

        assertTrue(old.isOlderThan(current));
        assertFalse(current.isOlderThan(old));
        assertFalse(VersionInfo.fromGit("git version 2.43").isOlderThan(VersionInfo.fromGit("git version 2.43.0")));
    }

    @Test
    void unknownVersionsAreNeverOlder() {
        VersionInfo unknown = VersionInfo.fromRemoting(null);
        VersionInfo known = VersionInfo.fromRemoting("3352.v17a_fb_4b_2773f");

        assertFalse(unknown.isOlderThan(known));
        assertFalse(known.isOlderThan(unknown));
        assertFalse(VersionInfo.fromRemoting("unknown").isOlderThan(known));
        assertFalse(unknown.isFeatureOlderThan(known));
    }

    @Test
    void unknownIsNotAvailable() {
        VersionInfo info = VersionInfo.fromRemoting(null);

        assertEquals("N/A", info.getDisplay());
        assertEquals("", info.getTooltip());
    }
}
