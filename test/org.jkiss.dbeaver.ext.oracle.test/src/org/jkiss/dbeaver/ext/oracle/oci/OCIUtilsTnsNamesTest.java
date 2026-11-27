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
package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

class OCIUtilsTnsNamesTest extends DBeaverUnitTest {

    @TempDir
    Path tnsDir;

    @Test
    void readsAliasesFromMainAndIncludedFiles() throws IOException {
        writeTnsFiles();
        Map<String, String> aliases = OCIUtils.readTnsNames(tnsDir.toFile(), false);
        Assertions.assertEquals(Set.of("ORCL_MAIN", "ORCL_EXTRA"), aliases.keySet());
        Assertions.assertTrue(aliases.get("ORCL_EXTRA").contains("PORT = 1522"));
    }

    @Test
    void tnsFilesAreReleasedAfterReading() throws IOException {
        // On Windows a file that is still open can't be renamed, so a leaked reader makes this fail.
        // Other platforms allow renaming open files, so there this only checks that reading succeeds.
        writeTnsFiles();
        OCIUtils.readTnsNames(tnsDir.toFile(), false);
        assertCanRename(tnsDir.resolve("tnsnames.ora"));
        assertCanRename(tnsDir.resolve("extra_tnsnames.ora"));
    }

    private void writeTnsFiles() throws IOException {
        Files.writeString(tnsDir.resolve("tnsnames.ora"), """
            ORCL_MAIN =
              (DESCRIPTION =
                (ADDRESS = (PROTOCOL = TCP)(HOST = localhost)(PORT = 1521))
                (CONNECT_DATA = (SERVICE_NAME = ORCLPDB1))
              )
            IFILE=extra_tnsnames.ora
            """);
        Files.writeString(tnsDir.resolve("extra_tnsnames.ora"), """
            ORCL_EXTRA =
              (DESCRIPTION =
                (ADDRESS = (PROTOCOL = TCP)(HOST = localhost)(PORT = 1522))
                (CONNECT_DATA = (SERVICE_NAME = ORCLPDB2))
              )
            """);
    }

    private static void assertCanRename(@NotNull Path file) {
        Path renamed = file.resolveSibling(file.getFileName() + ".bak");
        Assertions.assertDoesNotThrow(() -> Files.move(file, renamed), "File is still open: " + file);
    }
}
