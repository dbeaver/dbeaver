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
package org.jkiss.dbeaver;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LogOutputStreamTest extends DBeaverUnitTest {

    private static final File LOG_FILE = new File("logs", "debug.log");

    @Test
    public void rotatesExistingLogAndContinuesWritingToInitialFile() throws IOException {
        TestOperations operations = new TestOperations(100, 7);
        operations.addFile(LOG_FILE);

        try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
            Assertions.assertTrue(operations.hasFile("debug-1000.log"));
            Assertions.assertTrue(operations.diagnostics.isEmpty());

            output.write(1);
            Assertions.assertEquals(List.of(LOG_FILE), operations.openedFiles);
        }
    }

    @Test
    public void retriesTransientRenameFailure() throws IOException {
        TestOperations operations = new TestOperations(100, 7);
        operations.addFile(LOG_FILE);
        operations.renameResults.addAll(List.of(false, false, true));

        try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
            Assertions.assertEquals(3, operations.renameAttempts);
            Assertions.assertEquals(2, operations.sleepCount);
            Assertions.assertTrue(operations.hasFile("debug-1000.log"));
        }
    }

    @Test
    public void switchesToFallbackFileAfterPermanentRenameFailure() throws IOException {
        TestOperations operations = new TestOperations(100, 7);
        operations.addFile(LOG_FILE);
        operations.renameResults.addAll(List.of(false, false, false, false, false, false));

        try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
            Assertions.assertEquals(6, operations.renameAttempts);
            Assertions.assertEquals(5, operations.sleepCount);
            Assertions.assertTrue(
                operations.diagnostics.stream().anyMatch(message -> message.startsWith("Failed to rename log "))
            );

            output.write(1);
            Assertions.assertEquals(List.of(new File("logs", "debug-1000-since.log")), operations.openedFiles);
        }
    }

    @Test
    public void appliesRetentionToRegularAndFallbackArchives() throws IOException {
        TestOperations operations = new TestOperations(100, 2);
        operations.addFile(LOG_FILE);
        operations.addFile("debug-100.log");
        operations.addFile("debug-200-since.log");
        operations.addFile("debug-300.log");
        operations.timestamp = 400;

        try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
            Assertions.assertFalse(operations.hasFile("debug-100.log"));
            Assertions.assertFalse(operations.hasFile("debug-200-since.log"));
            Assertions.assertTrue(operations.hasFile("debug-300.log"));
            Assertions.assertTrue(operations.hasFile("debug-400.log"));
        }
    }

    @Test
    public void retainsNewestArchiveWhenInitialLogIsStuck() throws IOException {
        TestOperations operations = new TestOperations(100, 1);
        operations.addFile(LOG_FILE);
        operations.addFile("debug-100.log");
        operations.addFile("debug-200.log");
        operations.addFile("debug-300-since.log");
        operations.renameResults.addAll(List.of(false, false, false, false, false, false));

        try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
            Assertions.assertFalse(operations.hasFile("debug-100.log"));
            Assertions.assertFalse(operations.hasFile("debug-200.log"));
            Assertions.assertTrue(operations.hasFile("debug-300-since.log"));
        }
    }

    @Test
    public void retriesDirectoryListingAndDeletion() throws IOException {
        TestOperations operations = new TestOperations(100, 1);
        operations.addFile(LOG_FILE);
        operations.addFile("debug-100.log");
        operations.listFailures = 2;
        operations.deleteResults.addAll(List.of(false, true));
        operations.timestamp = 200;

        try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
            Assertions.assertEquals(3, operations.sleepCount);
            Assertions.assertEquals(2, operations.deleteAttempts);
            Assertions.assertFalse(operations.hasFile("debug-100.log"));
        }
    }

    @Test
    public void restoresInterruptAndStopsRetrying() throws IOException {
        TestOperations operations = new TestOperations(100, 7);
        operations.addFile(LOG_FILE);
        operations.renameResults.add(false);
        operations.interruptOnSleep = true;

        try {
            try (LogOutputStream output = new LogOutputStream(LOG_FILE, operations)) {
                Assertions.assertTrue(Thread.currentThread().isInterrupted());
                Assertions.assertEquals(1, operations.renameAttempts);
            }
        } finally {
            Thread.interrupted();
        }
    }

    private static class TestOperations implements LogOutputStream.Operations {
        private final DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        private final Set<File> files = new LinkedHashSet<>();
        private final Deque<Boolean> renameResults = new ArrayDeque<>();
        private final Deque<Boolean> deleteResults = new ArrayDeque<>();
        private final List<File> openedFiles = new ArrayList<>();
        private final List<String> diagnostics = new ArrayList<>();

        private long timestamp = 1000;
        private int renameAttempts;
        private int deleteAttempts;
        private int sleepCount;
        private int listFailures;
        private boolean interruptOnSleep;

        private TestOperations(long maxLogSize, int maxLogFiles) {
            Mockito.when(preferences.getLong(LogOutputStream.LOGS_MAX_FILE_SIZE)).thenReturn(maxLogSize);
            Mockito.when(preferences.getInt(LogOutputStream.LOGS_MAX_FILES_COUNT)).thenReturn(maxLogFiles);
        }

        private void addFile(@NotNull File file) {
            files.add(file);
        }

        private void addFile(@NotNull String name) {
            addFile(new File(LOG_FILE.getParentFile(), name));
        }

        private boolean hasFile(@NotNull String name) {
            return files.contains(new File(LOG_FILE.getParentFile(), name));
        }

        @NotNull
        @Override
        public DBPPreferenceStore getPreferences() {
            return preferences;
        }

        @Override
        public boolean exists(@NotNull File file) {
            return files.contains(file) || LOG_FILE.getParentFile().equals(file);
        }

        @Override
        public boolean isFile(@NotNull File file) {
            return files.contains(file);
        }

        @Override
        public boolean isDirectory(@NotNull File file) {
            return LOG_FILE.getParentFile().equals(file);
        }

        @Override
        public boolean makeDirectories(@NotNull File directory) {
            return false;
        }

        @Override
        public long length(@NotNull File file) {
            return 0;
        }

        @NotNull
        @Override
        public OutputStream openFile(@NotNull File file) {
            files.add(file);
            openedFiles.add(file);
            return new ByteArrayOutputStream();
        }

        @Override
        public boolean rename(@NotNull File source, @NotNull File target) {
            renameAttempts++;
            boolean result = renameResults.isEmpty() || renameResults.removeFirst();
            if (result) {
                files.remove(source);
                files.add(target);
            }
            return result;
        }

        @Nullable
        @Override
        public File[] listFiles(@NotNull File directory, @NotNull FilenameFilter filter) {
            if (listFailures > 0) {
                listFailures--;
                return null;
            }
            return files.stream()
                .filter(file -> directory.equals(file.getParentFile()))
                .filter(file -> filter.accept(directory, file.getName()))
                .toArray(File[]::new);
        }

        @Override
        public boolean delete(@NotNull File file) {
            deleteAttempts++;
            boolean result = deleteResults.isEmpty() || deleteResults.removeFirst();
            if (result) {
                files.remove(file);
            }
            return result;
        }

        @Override
        public long currentTimeMillis() {
            return timestamp++;
        }

        @Override
        public void threadSleep(@NotNull Duration duration) throws InterruptedException {
            sleepCount++;
            if (interruptOnSleep) {
                throw new InterruptedException();
            }
        }

        @Override
        public void debugPrint(@NotNull String message) {
            diagnostics.add(message);
        }
    }
}
