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
package org.jkiss.dbeaver.model.preferences;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfirmedConnectionInitScriptsStoreTest {
    @TempDir
    Path directory;

    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
    private Path file;
    private ConfirmedConnectionInitScriptsStore store;

    @BeforeEach
    public void setUp() {
        file = directory.resolve("settings/approvals.json");
        store = new ConfirmedConnectionInitScriptsStore(file);
        Mockito.when(container.getProject().getAbsolutePath()).thenReturn(directory.resolve("project"));
        Mockito.when(container.getId()).thenReturn("connection-id");
        Mockito.when(container.getDriver().getFullId()).thenReturn("h2:h2_embedded_v3");
    }

    @Test
    public void remembersApprovalAfterStoreIsRecreated() {
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
        store.add(container, "jdbc:h2:mem:test", "CALL 1");

        Assertions.assertTrue(new ConfirmedConnectionInitScriptsStore(file).contains(container, "jdbc:h2:mem:test", "CALL 1"));
    }

    @Test
    public void requiresApprovalWhenUrlOrSqlChanges() {
        store.add(container, "jdbc:h2:mem:test", "CALL 1");

        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:other", "CALL 1"));
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 2"));
        Assertions.assertFalse(store.contains(container, null, "CALL 1"));
    }

    @Test
    public void doesNotTransferApprovalToOtherConnectionsProjectsOrDrivers() {
        store.add(container, "jdbc:h2:mem:test", "CALL 1");
        Mockito.when(container.getId()).thenReturn("other-connection");
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
        Mockito.when(container.getId()).thenReturn("connection-id");
        Mockito.when(container.getProject().getAbsolutePath()).thenReturn(directory.resolve("other-project"));
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
        Mockito.when(container.getProject().getAbsolutePath()).thenReturn(directory.resolve("project"));
        Mockito.when(container.getDriver().getFullId()).thenReturn("other-driver");
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
    }

    @Test
    public void preservesApprovalsWrittenByAnotherStore() {
        store.add(container, "jdbc:h2:mem:test", "CALL 1");
        new ConfirmedConnectionInitScriptsStore(file).add(container, "jdbc:h2:mem:other", "CALL 2");
        store.add(container, null, "CALL 3");

        Assertions.assertTrue(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
        Assertions.assertTrue(store.contains(container, "jdbc:h2:mem:other", "CALL 2"));
        Assertions.assertTrue(store.contains(container, null, "CALL 3"));
        Assertions.assertFalse(store.contains(container, "", "CALL 3"));
    }

    @Test
    public void doesNotStoreSqlOrConnectionDetailsAsPlainText() throws Exception {
        store.add(container, "jdbc:h2:mem:test", "CALL 'private-sql'");

        String saved = Files.readString(file);
        Assertions.assertFalse(saved.contains("private-sql"));
        Assertions.assertFalse(saved.contains("jdbc:h2"));
        Assertions.assertFalse(saved.contains("connection-id"));
    }

    @Test
    public void rejectsApprovalAfterStoreIsDeletedOrCorrupted() throws Exception {
        store.add(container, "jdbc:h2:mem:test", "CALL 1");
        Files.delete(file);
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
        Files.writeString(file, "invalid json");
        Assertions.assertFalse(store.contains(container, "jdbc:h2:mem:test", "CALL 1"));
    }
}
