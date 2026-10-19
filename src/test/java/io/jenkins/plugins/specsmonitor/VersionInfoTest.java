package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void unknownIsNotAvailable() {
        VersionInfo info = VersionInfo.fromRemoting(null);

        assertEquals("N/A", info.getDisplay());
        assertEquals("", info.getTooltip());
    }
}
