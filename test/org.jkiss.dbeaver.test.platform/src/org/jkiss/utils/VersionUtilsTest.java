/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.utils;

import org.jkiss.dbeaver.utils.VersionUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for {@link VersionUtils} covering version comparison, beta detection,
 * version component extraction, and range checking.
 */
public class VersionUtilsTest {

    // ========== isBetaVersion ==========

    @Test
    public void testIsBetaVersionWithBeta() {
        assertTrue(VersionUtils.isBetaVersion("1.0.0-beta"));
        assertTrue(VersionUtils.isBetaVersion("1.0.0-beta.1"));
        assertTrue(VersionUtils.isBetaVersion("2.0-BETA"));
    }

    @Test
    public void testIsBetaVersionWithAlpha() {
        assertTrue(VersionUtils.isBetaVersion("1.0.0-alpha"));
        assertTrue(VersionUtils.isBetaVersion("3.5-ALPHA.2"));
    }

    @Test
    public void testIsBetaVersionStableRelease() {
        assertFalse(VersionUtils.isBetaVersion("1.0.0"));
        assertFalse(VersionUtils.isBetaVersion("24.3.1"));
        assertFalse(VersionUtils.isBetaVersion("1.0.0-rc1"));
    }

    // ========== compareVersions ==========

    @Test
    public void testCompareVersionsEqual() {
        assertEquals(0, VersionUtils.compareVersions("1.0.0", "1.0.0"));
        assertEquals(0, VersionUtils.compareVersions("24.3.1", "24.3.1"));
    }

    @Test
    public void testCompareVersionsLessThan() {
        assertTrue(VersionUtils.compareVersions("1.0.0", "1.0.1") < 0);
        assertTrue(VersionUtils.compareVersions("1.0.0", "2.0.0") < 0);
        assertTrue(VersionUtils.compareVersions("1.9.9", "2.0.0") < 0);
    }

    @Test
    public void testCompareVersionsGreaterThan() {
        assertTrue(VersionUtils.compareVersions("2.0.0", "1.0.0") > 0);
        assertTrue(VersionUtils.compareVersions("1.1.0", "1.0.9") > 0);
    }

    @Test
    public void testCompareVersionsDifferentLengths() {
        assertTrue(VersionUtils.compareVersions("1.0.0.1", "1.0.0") > 0);
        assertTrue(VersionUtils.compareVersions("1.0", "1.0.0") < 0);
    }

    @Test
    public void testCompareVersionsWithDashSeparator() {
        assertEquals(0, VersionUtils.compareVersions("1-0-0", "1.0.0"));
    }

    @Test
    public void testCompareVersionsWithUnderscoreSeparator() {
        assertEquals(0, VersionUtils.compareVersions("1_0_0", "1.0.0"));
    }

    @Test
    public void testCompareVersionsNonNumericTokens() {
        assertTrue(VersionUtils.compareVersions("1.0.0-alpha", "1.0.0-beta") < 0);
        assertTrue(VersionUtils.compareVersions("1.0.0-beta", "1.0.0-alpha") > 0);
        assertEquals(0, VersionUtils.compareVersions("1.0.0-rc", "1.0.0-rc"));
    }

    // ========== isVersionLessThan ==========

    @Test
    public void testIsVersionLessThan() {
        assertTrue(VersionUtils.isVersionLessThan("1.0.0", "2.0.0"));
        assertFalse(VersionUtils.isVersionLessThan("2.0.0", "1.0.0"));
        assertFalse(VersionUtils.isVersionLessThan("1.0.0", "1.0.0"));
    }

    // ========== isVersionGreaterThanOrEqual ==========

    @Test
    public void testIsVersionGreaterThanOrEqual() {
        assertTrue(VersionUtils.isVersionGreaterThanOrEqual("2.0.0", "1.0.0"));
        assertTrue(VersionUtils.isVersionGreaterThanOrEqual("1.0.0", "1.0.0"));
        assertFalse(VersionUtils.isVersionGreaterThanOrEqual("1.0.0", "2.0.0"));
    }

    // ========== findLatestVersion ==========

    @Test
    public void testFindLatestVersionStable() {
        List<String> versions = Arrays.asList("1.0.0", "2.0.0", "1.5.0");
        assertEquals("2.0.0", VersionUtils.findLatestVersion(versions));
    }

    @Test
    public void testFindLatestVersionSkipsBeta() {
        List<String> versions = Arrays.asList("1.0.0", "3.0.0-beta", "2.0.0");
        assertEquals("2.0.0", VersionUtils.findLatestVersion(versions));
    }

    @Test
    public void testFindLatestVersionFallsToBetaWhenNoStable() {
        List<String> versions = Arrays.asList("1.0.0-alpha", "2.0.0-beta", "1.5.0-beta");
        assertEquals("2.0.0-beta", VersionUtils.findLatestVersion(versions));
    }

    @Test
    public void testFindLatestVersionEmptyCollection() {
        assertNull(VersionUtils.findLatestVersion(Collections.emptyList()));
    }

    @Test
    public void testFindLatestVersionSkipsNullAndEmpty() {
        List<String> versions = Arrays.asList(null, "", "1.0.0", "2.0.0");
        assertEquals("2.0.0", VersionUtils.findLatestVersion(versions));
    }

    // ========== getMajorVersion ==========

    @Test
    public void testGetMajorVersion() {
        assertEquals(24, VersionUtils.getMajorVersion("24.3.1"));
        assertEquals(1, VersionUtils.getMajorVersion("1.0"));
        assertEquals(0, VersionUtils.getMajorVersion("beta"));
    }

    // ========== getMinorVersion ==========

    @Test
    public void testGetMinorVersion() {
        assertEquals(3, VersionUtils.getMinorVersion("24.3.1"));
        assertEquals(0, VersionUtils.getMinorVersion("1.0"));
        assertEquals(0, VersionUtils.getMinorVersion("1"));
    }

    // ========== getPatchVersion ==========

    @Test
    public void testGetPatchVersion() {
        assertEquals(1, VersionUtils.getPatchVersion("24.3.1"));
        assertEquals(0, VersionUtils.getPatchVersion("24.3"));
        assertEquals(0, VersionUtils.getPatchVersion("24"));
    }

    // ========== isVersionInRange ==========

    @Test
    public void testIsVersionInRangeWithinRange() {
        assertTrue(VersionUtils.isVersionInRange("1.5.0", "1.0.0", "2.0.0"));
    }

    @Test
    public void testIsVersionInRangeAtBoundaries() {
        assertTrue(VersionUtils.isVersionInRange("1.0.0", "1.0.0", "2.0.0"));
        assertTrue(VersionUtils.isVersionInRange("2.0.0", "1.0.0", "2.0.0"));
    }

    @Test
    public void testIsVersionInRangeOutOfRange() {
        assertFalse(VersionUtils.isVersionInRange("0.9.0", "1.0.0", "2.0.0"));
        assertFalse(VersionUtils.isVersionInRange("3.0.0", "1.0.0", "2.0.0"));
    }

    @Test
    public void testIsVersionInRangeNullBounds() {
        assertTrue(VersionUtils.isVersionInRange("5.0.0", null, null));
        assertTrue(VersionUtils.isVersionInRange("1.0.0", null, "2.0.0"));
        assertTrue(VersionUtils.isVersionInRange("3.0.0", "1.0.0", null));
    }
}
