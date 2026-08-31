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
package org.jkiss.dbeaver.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LauncherUtilsTest {

    private static final String UNC_SERVER = "filer.domain.tld";

    @Test
    public void roundTripPreservesPathWithSpaces() throws Exception {
        File original = new File("/home/john smith/DBeaverData");
        File decoded = LauncherUtils.toFile(LauncherUtils.toURL(original));
        assertEquals(original.getAbsoluteFile(), decoded.getAbsoluteFile());
    }

    @Test
    public void roundTripPreservesPlainLocalPath() throws Exception {
        File original = new File("/tmp/dbeaver/install-data/workspace6");
        File decoded = LauncherUtils.toFile(LauncherUtils.toURL(original));
        assertEquals(original.getAbsoluteFile(), decoded.getAbsoluteFile());
    }

    @Test
    public void roundTripPreservesPathWithSpacesWhenEncodedInUrl() throws Exception {
        URL url = LauncherUtils.toURL(new File("/tmp/a b"));
        File decoded = LauncherUtils.toFile(url);
        assertEquals(new File("/tmp/a b").getAbsoluteFile(), decoded.getAbsoluteFile());
    }

    @Test
    public void toFileHandlesLegacyUnescapedSpaceUrl() throws Exception {
        URL legacy = new URL("file:/home/john smith/DBeaverData");
        assertEquals(new File("/home/john smith/DBeaverData").getPath(), LauncherUtils.toFile(legacy).getPath());
    }

    @Test
    public void toFileReconstructsUncFromLegacyAuthorityUrl() throws Exception {
        URL legacy = new URL("file://" + UNC_SERVER + "/private/joe/AppData2022");
        File decoded = LauncherUtils.toFile(legacy);
        String path = decoded.getPath();
        assertEquals(UNC_SERVER + "/private/joe/AppData2022", path.substring(path.indexOf(UNC_SERVER)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void roundTripPreservesUncPathOnWindows() throws Exception {
        File unc = new File("\\\\" + UNC_SERVER + "\\private\\joe\\AppData2022");
        URL url = LauncherUtils.toURL(unc);
        assertEquals("", url.getHost());
        File decoded = LauncherUtils.toFile(url);
        assertEquals(unc.getAbsoluteFile(), decoded.getAbsoluteFile());
    }

    @Test
    public void toFileRejectsNonFileUrl() {
        assertThrows(IllegalArgumentException.class, () -> LauncherUtils.toFile(new URL("http://example.com/x")));
    }
}
