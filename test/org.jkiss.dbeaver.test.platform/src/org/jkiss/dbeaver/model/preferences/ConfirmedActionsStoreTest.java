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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfirmedActionsStoreTest {
    @TempDir
    Path directory;

    @Test
    public void preservesStoredApprovals() throws Exception {
        Path file = directory.resolve("actions.json");
        Files.writeString(file, "[\"echo first\", \"echo second\"]");
        ConfirmedActionsStore store = new ConfirmedActionsStore(file);
        Assertions.assertTrue(store.contains("echo first"));
        Assertions.assertTrue(store.add("echo third"));
        Assertions.assertFalse(store.add("echo third"));
        Assertions.assertTrue(new ConfirmedActionsStore(file).contains("echo second"));
        Assertions.assertTrue(Files.readString(file).stripLeading().startsWith("["));
        Assertions.assertFalse(store.contains("echo first && echo other"));
    }

    @Test
    public void seesExternalChanges() throws Exception {
        Path file = directory.resolve("commands.json");
        ConfirmedActionsStore store = new ConfirmedActionsStore(file);
        store.add("echo first");
        Files.writeString(file, "[\"echo replacement\"]");
        Assertions.assertFalse(store.contains("echo first"));
        Assertions.assertTrue(store.contains("echo replacement"));
    }

    @Test
    public void preservesApprovalsWrittenByAnotherStore() throws Exception {
        Path file = directory.resolve("actions.json");
        ConfirmedActionsStore store = new ConfirmedActionsStore(file);
        store.add("first");
        new ConfirmedActionsStore(file).add("second");
        store.add("third");

        Assertions.assertTrue(store.contains("first"));
        Assertions.assertTrue(store.contains("second"));
        Assertions.assertTrue(store.contains("third"));
    }

    @Test
    public void rejectsApprovalsAfterStoreIsDeletedOrCorrupted() throws Exception {
        Path file = directory.resolve("actions.json");
        ConfirmedActionsStore store = new ConfirmedActionsStore(file);
        store.add("first");
        Files.delete(file);
        Assertions.assertFalse(store.contains("first"));
        Files.writeString(file, "invalid json");
        Assertions.assertFalse(store.contains("first"));
    }

    @Test
    public void replacesOnlyTheApprovalForTheChangedConnection() throws Exception {
        ConfirmedActionsStore store = new ConfirmedActionsStore(directory.resolve("actions.json"));
        store.add("first:old");
        store.add("second:trusted");
        store.replaceWithPrefix("first:", "first:new");
        Assertions.assertFalse(store.contains("first:old"));
        Assertions.assertTrue(store.contains("first:new"));
        Assertions.assertTrue(store.contains("second:trusted"));
        store.replaceWithPrefix("first:", null);
        Assertions.assertFalse(store.contains("first:new"));
        Assertions.assertTrue(store.contains("second:trusted"));
    }

    @Test
    public void ioFailuresDoNotReturnARecordedApproval() throws Exception {
        Assertions.assertThrows(org.jkiss.dbeaver.DBException.class,
            () -> new ConfirmedActionsStore(directory).contains("echo first"));
    }
}
