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
        assertEquals("Patch release notes", patches.getFirst().releaseNotes());
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

    @NotNull
    private static VersionDescriptor parse(@NotNull String xml) throws IOException {
        return new VersionDescriptor(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
