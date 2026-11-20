/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.updater;

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class VersionDescriptorTest extends DBeaverUnitTest {

    @Test
    public void testPatchMetadataDoesNotOverwriteRelease() throws IOException {
        VersionDescriptor descriptor = parse("""
            <version>
                <number>26.2.0</number>
                <date>07.09.2026</date>
                <release-notes><![CDATA[Base release notes]]></release-notes>
                <patches>
                    <patch>
                        <patch-version>1</patch-version>
                        <patch-date>03.11.2026</patch-date>
                        <patch-release-notes><![CDATA[Patch release notes]]></patch-release-notes>
                    </patch>
                </patches>
            </version>
            """);

        assertEquals(new Version("26.2.0"), descriptor.getProgramVersion());
        assertEquals("07.09.2026", descriptor.getUpdateTime());
        assertEquals("Base release notes", descriptor.getReleaseNotes());
        assertEquals("26.2.0.1", descriptor.getUpdateVersion());
        assertEquals("03.11.2026", descriptor.getPatchUpdateTime());
        var patches = descriptor.getApplicablePatches(new Version("26.2.0"), 0);
        assertEquals(1, patches.size());
        assertEquals(1, patches.getFirst().number());
        assertEquals("03.11.2026", patches.getFirst().date());
        assertEquals("Patch release notes", patches.getFirst().releaseNotes());
        assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(new Version("26.2.0"), 0));
        assertFalse(descriptor.hasCriticalUpdates(new Version("26.2.0"), 0));
        assertTrue(descriptor.isNewerThan(new Version("23.3.0"), 0));
        assertTrue(descriptor.isPatchUpdate(new Version("26.2.0"), 0));
    }

    @Test
    public void testDuplicatePatchVersionIsRejected() {
        assertThrows(IOException.class, () -> parse("""
            <version>
                <number>26.2.0</number>
                <patches>
                    <patch>
                        <patch-version>1</patch-version>
                        <patch-version>2</patch-version>
                    </patch>
                </patches>
            </version>
            """));
    }

    @Test
    public void testMajorUpdateClassification() throws IOException {
        VersionDescriptor descriptor = parse("<version><number>26.2.0</number></version>");
        assertTrue(descriptor.isMajorUpdate());

        descriptor.setProgramVersion(new Version("26.2.0.beta"));
        assertTrue(descriptor.isMajorUpdate());

        descriptor.setProgramVersion(new Version("26.2.1"));
        assertFalse(descriptor.isMajorUpdate());
        descriptor.setProgramVersion(new Version("27.0.1"));
        assertFalse(descriptor.isMajorUpdate());
    }

    @Test
    public void testPatchesInheritReleaseClassification() throws IOException {
        VersionDescriptor descriptor = parse("""
            <version>
                <number>26.2.0</number>
                <patches><patch><patch-version>1</patch-version></patch></patches>
            </version>
            """);
        assertEquals("26.2.0.1", descriptor.getUpdateVersion());
        assertTrue(descriptor.isMajorUpdate());

        descriptor.setProgramVersion(new Version("26.2.1"));
        assertEquals("26.2.1.1", descriptor.getUpdateVersion());
        assertFalse(descriptor.isMajorUpdate());
    }

    @Test
    public void testAbsentCriticalUpdates() throws IOException {
        VersionDescriptor descriptor = parse("<version><number>26.2.0</number></version>");
        Version installed = new Version("26.1.0");
        assertTrue(descriptor.getCriticalUpdates().isEmpty());
        assertTrue(descriptor.getApplicableCriticalUpdates(installed, 0).isEmpty());
        assertFalse(descriptor.hasCriticalUpdates(installed, 0));
        assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(installed, 0));
    }

    @Test
    public void testCriticalUpdateParsingDepthAndCdata() throws IOException {
        VersionDescriptor descriptor = parse("""
            <version>
                <number>26.2.0</number>
                <date>07.09.2026</date>
                <release-notes>Base notes</release-notes>
                <wrapper>
                    <critical-update>
                        <update update-date="03.11.2026" fixed-in="99.0.0" severity="critical">Ignored</update>
                    </critical-update>
                </wrapper>
                <update update-date="03.11.2026" fixed-in="99.0.0" severity="critical">Ignored</update>
                <critical-update>
                    <wrapper>
                        <update update-date="03.11.2026" fixed-in="99.0.0" severity="critical">Ignored</update>
                    </wrapper>
                    <update update-date="03.11.2026" fixed-in="26.2.0" severity="critical">%s</update>
                </critical-update>
            </version>
            """.formatted("<![CDATA[Before <b>&</b> ]]><number>nested</number>"
                + "<date>date</date><release-notes>notes</release-notes><![CDATA[ after]]>"));
        assertEquals(new Version("26.2.0"), descriptor.getProgramVersion());
        assertEquals("07.09.2026", descriptor.getUpdateTime());
        assertEquals("Base notes", descriptor.getReleaseNotes());
        assertEquals(1, descriptor.getCriticalUpdates().size());
        var update = descriptor.getCriticalUpdates().getFirst();
        assertEquals(new Version("26.2.0"), update.fixedIn());
        assertEquals("03.11.2026", new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(update.updateDate()));
        assertEquals(VersionDescriptor.PatchSeverity.CRITICAL, update.severity());
        assertEquals("Before <b>&</b> nesteddatenotes after", update.description());
    }

    @Test
    public void testMalformedCriticalUpdatesAreSkipped() throws IOException {
        for (String attributes : List.of(
            "fixed-in='26.2.0' severity='critical'",
            "update-date='03.11.2026' severity='critical'",
            "update-date='03.11.2026' fixed-in='26.2.0'",
            "update-date='03.11.2026' fixed-in='26.2' severity='critical'",
            "update-date='03.11.2026' fixed-in='26.2.0.beta' severity='critical'",
            "update-date='03.11.2026' fixed-in='26.2.0.-1' severity='critical'",
            "update-date='03.11.2026' fixed-in='26.2.0.2147483648' severity='critical'",
            "update-date='03.11.2026' fixed-in='2147483648.2.0' severity='critical'"
        )) {
            VersionDescriptor descriptor = parse("""
                <version>
                    <number>26.2.0</number>
                    <critical-update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.1" severity="medium">First</update>
                        <update %s>Invalid</update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.2" severity="critical">Last</update>
                    </critical-update>
                    <patches><patch><patch-version>3</patch-version><severity>high</severity></patch></patches>
                </version>
                """.formatted(attributes));
            assertEquals(List.of("First", "Last"), descriptor.getCriticalUpdates().stream()
                .map(VersionDescriptor.CriticalUpdate::description).toList(), attributes);
            assertEquals("26.2.0.3", descriptor.getUpdateVersion());
        }
    }

    @Test
    public void testGeneratedLegacyPatchSeveritiesAreRetainedAsNone() throws IOException {
        Version installed = new Version("26.2.0");
        for (String severity : List.of("major", "", "   ", "unknown")) {
            VersionDescriptor descriptor = parse("""
                <version><number>26.2.0</number>
                    <critical-update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.1" severity="critical">Fixed</update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.2" severity="%s">Legacy patch</update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.3">Missing attribute</update>
                    </critical-update>
                    <patches>
                        <patch><patch-version>2</patch-version><severity>%s</severity></patch>
                        <patch><patch-version>3</patch-version></patch>
                    </patches>
                </version>
                """.formatted(severity, severity));
            assertEquals(List.of("Fixed", "Legacy patch"), descriptor.getCriticalUpdates().stream()
                .map(VersionDescriptor.CriticalUpdate::description).toList(), severity);
            var applicable = descriptor.getApplicableCriticalUpdates(installed, 1);
            assertEquals(1, applicable.size(), severity);
            assertEquals("Legacy patch", applicable.getFirst().description());
            assertEquals(VersionDescriptor.PatchSeverity.NONE, applicable.getFirst().severity());
            assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(installed, 1));
            assertFalse(descriptor.hasCriticalUpdates(installed, 1), severity);
            assertTrue(descriptor.hasCriticalUpdates(installed, 0));
            assertEquals(VersionDescriptor.PatchSeverity.CRITICAL, descriptor.getUpdateSeverity(installed, 0));
        }
    }

    @Test
    public void testBlankCriticalUpdateDescriptionsAreSkipped() throws IOException {
        for (String description : List.of("", " \t\n ", "<![CDATA[ \t\n ]]>", "<details> \t\n </details>")) {
            VersionDescriptor descriptor = parse("""
                <version><number>26.2.0</number>
                    <critical-update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.1" severity="medium">First</update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.2" severity="critical">%s</update>
                        <update update-date="03.11.2026" fixed-in="26.2.0.3" severity="high">Last</update>
                    </critical-update>
                    <patches><patch><patch-version>3</patch-version></patch></patches>
                </version>
                """.formatted(description));
            assertEquals(List.of("First", "Last"), descriptor.getCriticalUpdates().stream()
                .map(VersionDescriptor.CriticalUpdate::description).toList(), description);
            assertFalse(descriptor.hasCriticalUpdates(new Version("26.2.0"), 0));
            assertEquals("26.2.0.3", descriptor.getUpdateVersion());
        }
    }

    @Test
    public void testCriticalUpdateDatesAreStrict() throws IOException {
        for (String date : List.of("31.11.2026", "29.02.2026", "00.11.2026", "03.13.2026", "3.11.2026",
            "03.11.26", "03.11.2026extra", "03.11.0000", "")) {
            VersionDescriptor descriptor = parse("""
                <version><number>26.2.0</number><critical-update>
                    <update update-date="%s" fixed-in="26.2.0" severity="critical">Invalid</update>
                    <update update-date="29.02.2024" fixed-in="26.2.0" severity="critical">Valid leap day</update>
                </critical-update></version>
                """.formatted(date));
            assertEquals(1, descriptor.getCriticalUpdates().size(), date);
            assertEquals("Valid leap day", descriptor.getCriticalUpdates().getFirst().description());
        }
    }

    @Test
    public void testCriticalUpdateNumericPatchComparison() throws IOException {
        VersionDescriptor descriptor = criticalUpdate("26.2.0.10", "critical");
        Version installed = new Version("26.2.0.202611030001");
        assertTrue(descriptor.hasCriticalUpdates(installed, 2));
        assertEquals(1, descriptor.getApplicableCriticalUpdates(installed, 9).size());
        assertFalse(descriptor.hasCriticalUpdates(installed, 10));
        assertFalse(descriptor.hasCriticalUpdates(installed, 11));
        assertFalse(criticalUpdate("26.2.0.2", "critical").hasCriticalUpdates(installed, 10));
    }

    @Test
    public void testCriticalUpdateBaseComparisonIgnoresBuildQualifier() throws IOException {
        VersionDescriptor descriptor = criticalUpdate("26.2.0", "critical");
        for (String version : List.of("25.10.9", "26.1.99", "26.1.99.202611030001")) {
            assertTrue(descriptor.hasCriticalUpdates(new Version(version), 99), version);
        }
        for (String version : List.of("26.2.0", "26.2.0.beta", "26.2.0.202611030001", "26.2.1", "26.10.0", "27.0.0")) {
            assertFalse(descriptor.hasCriticalUpdates(new Version(version), 0), version);
            assertTrue(descriptor.getApplicableCriticalUpdates(new Version(version), 0).isEmpty(), version);
        }
        assertTrue(criticalUpdate("26.2.0.1", "critical").hasCriticalUpdates(new Version("26.2.0.beta"), 0));
        assertTrue(criticalUpdate("26.10.0", "critical").hasCriticalUpdates(new Version("26.2.0"), 0));
    }

    @Test
    public void testCriticalUpdateSeverityLevels() throws IOException {
        Version installed = new Version("26.1.0");
        for (VersionDescriptor.PatchSeverity severity : VersionDescriptor.PatchSeverity.values()) {
            VersionDescriptor descriptor = criticalUpdate("26.2.0", " " + severity.name().toLowerCase(Locale.ROOT) + " ");
            assertEquals(1, descriptor.getApplicableCriticalUpdates(installed, 0).size());
            assertEquals(severity, descriptor.getCriticalUpdates().getFirst().severity());
            assertEquals(severity, descriptor.getUpdateSeverity(installed, 0));
            assertEquals(severity == VersionDescriptor.PatchSeverity.CRITICAL, descriptor.hasCriticalUpdates(installed, 0));
            assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(new Version("26.2.0"), 0));
        }
    }

    @Test
    public void testLegacyPatchSeverityDoesNotEscalateApplicableAdvisories() throws IOException {
        VersionDescriptor descriptor = parse("""
            <version><number>26.2.0</number>
                <critical-update>
                    <update update-date="03.11.2026" fixed-in="26.1.0" severity="critical">Already fixed</update>
                    <update update-date="03.11.2026" fixed-in="26.2.0.10" severity="medium">Medium</update>
                    <update update-date="03.11.2026" fixed-in="26.2.0.2" severity="high">High</update>
                </critical-update>
                <patches><patch><patch-version>3</patch-version><severity>critical</severity></patch></patches>
            </version>
            """);
        Version installed = new Version("26.2.0");
        assertEquals(List.of("Medium", "High"), descriptor.getApplicableCriticalUpdates(installed, 0).stream()
            .map(VersionDescriptor.CriticalUpdate::description).toList());
        assertFalse(descriptor.hasCriticalUpdates(installed, 0));
        assertEquals(VersionDescriptor.PatchSeverity.HIGH, descriptor.getUpdateSeverity(installed, 0));
        assertEquals(VersionDescriptor.PatchSeverity.MEDIUM, descriptor.getUpdateSeverity(installed, 2));
        assertFalse(descriptor.hasCriticalUpdates(installed, 2));
        assertEquals(VersionDescriptor.PatchSeverity.MEDIUM, descriptor.getUpdateSeverity(installed, 3));
        assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(installed, 10));
        assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(new Version("26.2.1"), 0));
    }

    @Test
    public void testLegacyPatchSeverityIsIgnoredWithoutAdvisories() throws IOException {
        Version installed = new Version("26.2.0");
        for (String severity : List.of("critical", "unknown")) {
            VersionDescriptor descriptor = parse("""
                <version><number>26.2.0</number>
                    <patches><patch>
                        <patch-version>1</patch-version>
                        <patch-date>03.11.2026</patch-date>
                        <severity>%s</severity>
                        <patch-release-notes><![CDATA[Patch release notes]]></patch-release-notes>
                    </patch></patches>
                </version>
                """.formatted(severity));
            assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(installed, 0), severity);
            assertFalse(descriptor.hasCriticalUpdates(installed, 0), severity);
            assertTrue(descriptor.getCriticalUpdates().isEmpty());
            assertEquals("26.2.0.1", descriptor.getUpdateVersion());
            assertEquals("03.11.2026", descriptor.getPatchUpdateTime());
            assertEquals(List.of(new VersionDescriptor.VersionPatch(1, "03.11.2026", "Patch release notes")),
                descriptor.getApplicablePatches(installed, 0));
        }
    }

    @Test
    public void testCriticalAdvisoryForPatchUsesInstalledPatchFiltering() throws IOException {
        VersionDescriptor descriptor = parse("""
            <version><number>26.2.0</number>
                <critical-update>
                    <update update-date="03.11.2026" fixed-in="26.2.0.2" severity="critical">Fixed in patch 2</update>
                </critical-update>
                <patches><patch><patch-version>2</patch-version><severity>critical</severity></patch></patches>
            </version>
            """);
        Version installed = new Version("26.2.0.202611030001");
        assertTrue(descriptor.hasCriticalUpdates(installed, 1));
        assertEquals(VersionDescriptor.PatchSeverity.CRITICAL, descriptor.getUpdateSeverity(installed, 1));
        for (int installedPatch : List.of(2, 3)) {
            assertFalse(descriptor.hasCriticalUpdates(installed, installedPatch));
            assertEquals(VersionDescriptor.PatchSeverity.NONE, descriptor.getUpdateSeverity(installed, installedPatch));
            assertTrue(descriptor.getApplicableCriticalUpdates(installed, installedPatch).isEmpty());
        }
    }

    @Test
    public void testCriticalUpdatesAreImmutable() throws IOException {
        VersionDescriptor descriptor = criticalUpdate("26.2.0", "critical");
        assertThrows(UnsupportedOperationException.class, () -> descriptor.getCriticalUpdates().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> descriptor.getApplicableCriticalUpdates(new Version("26.1.0"), 0).clear());
        var update = descriptor.getCriticalUpdates().getFirst();
        Date original = update.updateDate();
        update.updateDate().setTime(0);
        assertEquals(original, update.updateDate());
        var copy = new VersionDescriptor.CriticalUpdate(original, update.fixedIn(), update.severity(), update.description());
        original.setTime(0);
        assertEquals(update.updateDate(), copy.updateDate());
    }

    @Test
    public void testOptionalCriticalUpdatesDoNotRelaxPatchValidation() {
        for (String patches : List.of(
            "<patch><patch-version>invalid</patch-version></patch>",
            "<patch><patch-version>0</patch-version></patch>",
            "<patch><patch-version>1</patch-version></patch><patch><patch-version>1</patch-version></patch>"
        )) {
            assertThrows(IOException.class, () -> parse("""
                <version><number>26.2.0</number>
                    <critical-update><update>Invalid optional entry</update></critical-update>
                    <patches>%s</patches>
                </version>
                """.formatted(patches)));
        }
    }

    @NotNull
    private static VersionDescriptor criticalUpdate(@NotNull String fixedIn, @NotNull String severity) throws IOException {
        return parse("""
            <version><number>26.2.0</number><critical-update>
                <update update-date="03.11.2026" fixed-in="%s" severity="%s"><![CDATA[Description]]></update>
            </critical-update></version>
            """.formatted(fixedIn, severity));
    }

    @NotNull
    private static VersionDescriptor parse(@NotNull String xml) throws IOException {
        return new VersionDescriptor(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
