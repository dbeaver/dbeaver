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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankFile;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * {@link MimerDatabankFileManager#canCreateObject}/{@code canDeleteObject} - multi-file databanks
 * are Mimer SQL 11.0+ only (both refuse outright on an older server, see {@link
 * MimerDataSource#supportsMultiFileDatabanks}), and a databank's last remaining file can never be
 * dropped (Mimer SQL needs at least one, and {@code DROP FILE} has nothing to transfer its data
 * to).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankFileManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerDatabank databank;

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private MimerDatabankFile file;

    @Mock
    private DBSObjectCache<MimerDatabank, MimerDatabankFile> fileCache;

    private final MimerDatabankFileManager manager = new MimerDatabankFileManager();

    @Test
    public void refusesCreateOnAServerWithoutMultiFileDatabankSupport() {
        when(databank.getDataSource()).thenReturn(dataSource);
        when(dataSource.supportsMultiFileDatabanks()).thenReturn(false);

        Assertions.assertFalse(manager.canCreateObject(databank));
    }

    @Test
    public void doesNotRefuseCreateBasedOnVersionAloneOnA110Server() {
        when(databank.getDataSource()).thenReturn(dataSource);
        when(dataSource.supportsMultiFileDatabanks()).thenReturn(true);
        // As above (MimerGroupMemberManagerTest) - only the version short-circuit is ours to
        // assert; the real permission check beyond that isn't controlled by this unit test.
        Assertions.assertDoesNotThrow(() -> manager.canCreateObject(databank));
    }

    @Test
    public void refusesDeleteOnAServerWithoutMultiFileDatabankSupport() {
        when(file.getDatabank()).thenReturn(databank);
        when(databank.getDataSource()).thenReturn(dataSource);
        when(dataSource.supportsMultiFileDatabanks()).thenReturn(false);

        Assertions.assertFalse(manager.canDeleteObject(file));
    }

    @Test
    public void refusesDeleteOfTheLastRemainingFile() {
        when(file.getDatabank()).thenReturn(databank);
        when(databank.getDataSource()).thenReturn(dataSource);
        when(dataSource.supportsMultiFileDatabanks()).thenReturn(true);
        when(databank.getFileCache()).thenReturn(fileCache);
        when(fileCache.getCachedObjects()).thenReturn(List.of(file));

        Assertions.assertFalse(manager.canDeleteObject(file));
    }

    @Test
    public void allowsDeleteWhenMoreThanOneFileExists() {
        MimerDatabankFile otherFile = org.mockito.Mockito.mock(MimerDatabankFile.class);
        when(file.getDatabank()).thenReturn(databank);
        when(databank.getDataSource()).thenReturn(dataSource);
        when(dataSource.supportsMultiFileDatabanks()).thenReturn(true);
        when(databank.getFileCache()).thenReturn(fileCache);
        when(fileCache.getCachedObjects()).thenReturn(List.of(file, otherFile));

        // Past the file-count/version gates, canDeleteObject() falls through to the same live
        // permission check as canCreateObject() - not asserted here for the same reason.
        Assertions.assertDoesNotThrow(() -> manager.canDeleteObject(file));
    }
}
