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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link MimerDatabankFile}'s DDL builders - {@code ALTER DATABANK ... ADD FILE} / {@code DROP FILE},
 * the per-file {@code ALTER DATABANK "db" ALTER FILE '<file>' SET/DROP <size>} form
 * ({@link MimerDatabankFile#buildAlterFileDDL}), and the file rename
 * ({@link MimerDatabankFile#buildRenameFileDDL}, which switches to the file-less
 * {@code ALTER DATABANK SET FILE} form for a single-file databank).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankFileTest extends DBeaverUnitTest {

    @Mock
    private MimerDatabank databank;

    @BeforeEach
    public void setUp() {
        when(databank.getName()).thenReturn("abc");
        lenient().when(databank.isSingleFile()).thenReturn(false); // multi-file unless a test overrides
    }

    private MimerDatabankFile newFile() {
        return new MimerDatabankFile(databank, "a2.dbf");
    }

    @Test
    public void addFileDDLListsOnlyTheSizeClausesThatAreSet() {
        MimerDatabankFile file = newFile();
        file.setFileSize("200M");
        file.setMaxSize("1G");
        Assertions.assertEquals(
            "ALTER DATABANK \"abc\" ADD FILE 'a2.dbf', FILESIZE 200M, MAXSIZE 1G",
            file.buildAddFileDDL());
    }

    @Test
    public void dropFileDDL() {
        Assertions.assertEquals("ALTER DATABANK \"abc\" DROP FILE 'a2.dbf'", newFile().buildDropFileDDL());
    }

    @Test
    public void alterFileDDLIsEmptyWhenNothingChanged() {
        Assertions.assertTrue(newFile().buildAlterFileDDL(Set.of()).isEmpty());
    }

    @Test
    public void alterFileDDLSetsChangedSizesViaAlterFile() {
        MimerDatabankFile file = newFile();
        file.setFileSize("200M");
        file.setGoalSize("500M");

        List<String> statements = file.buildAlterFileDDL(Set.of("fileSize", "goalSize"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertEquals(
            "ALTER DATABANK \"abc\" ALTER FILE 'a2.dbf' SET FILESIZE 200M, GOALSIZE 500M",
            statements.get(0));
    }

    @Test
    public void alterFileDDLDropsASizeClearedBackToBlankAndSplitsSetFromDrop() {
        MimerDatabankFile file = newFile();
        file.setMinSize("10M");   // set
        file.setMaxSize("");      // cleared -> DROP

        List<String> statements = file.buildAlterFileDDL(Set.of("minSize", "maxSize"));

        Assertions.assertEquals(2, statements.size());
        Assertions.assertEquals("ALTER DATABANK \"abc\" ALTER FILE 'a2.dbf' SET MINSIZE 10M", statements.get(0));
        Assertions.assertEquals("ALTER DATABANK \"abc\" ALTER FILE 'a2.dbf' DROP MAXSIZE", statements.get(1));
    }

    @Test
    public void renameFileDDLTargetsTheLoadedNameOnAMultiFileDatabank() {
        MimerDatabankFile file = newFile(); // created name "a2.dbf" == loaded name
        Assertions.assertNull(file.buildRenameFileDDL()); // unchanged

        file.setName("aa2.dbf");
        Assertions.assertEquals(
            "ALTER DATABANK \"abc\" ALTER FILE 'a2.dbf' SET FILE 'aa2.dbf'",
            file.buildRenameFileDDL());

        // After a successful rename the loaded name resyncs, so a second edit targets the new file.
        file.resyncLoadedFileName();
        file.setName("aa3.dbf");
        Assertions.assertEquals(
            "ALTER DATABANK \"abc\" ALTER FILE 'aa2.dbf' SET FILE 'aa3.dbf'",
            file.buildRenameFileDDL());
    }

    @Test
    public void renameFileDDLUsesTheFileLessFormOnASingleFileDatabank() {
        when(databank.isSingleFile()).thenReturn(true);
        MimerDatabankFile file = newFile();
        file.setName("aa2.dbf");
        Assertions.assertEquals(
            "ALTER DATABANK \"abc\" SET FILE 'aa2.dbf'",
            file.buildRenameFileDDL());
    }

    @Test
    public void alterFileDDLUsesTheFileLessFormOnASingleFileDatabank() {
        when(databank.isSingleFile()).thenReturn(true);
        MimerDatabankFile file = newFile();
        file.setFileSize("200M");

        List<String> statements = file.buildAlterFileDDL(Set.of("fileSize"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertEquals("ALTER DATABANK \"abc\" SET FILESIZE 200M", statements.get(0));
    }
}
