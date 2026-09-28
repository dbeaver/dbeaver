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
package org.jkiss.dbeaver.registry.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TaskRunStorageTest extends DBeaverUnitTest {
    private static final String PROJECT_ID = "history-project";
    private static final String TASK_ID = "history-task";
    private static final long START_TIME = 1_800_000_000_000L;

    @TempDir
    Path statisticsFolder;

    private DBPProject project;
    private DBTTaskType taskType;
    private DBTTaskRunStorage storage;
    private MockedStatic<TaskRegistry> taskRegistry;

    @BeforeEach
    public void setUp() {
        project = mock(DBPProject.class);
        taskType = mock(DBTTaskType.class);
        DBTTaskManager manager = mock(DBTTaskManager.class);
        storage = mock(DBTTaskRunStorage.class);
        when(project.getId()).thenReturn(PROJECT_ID);
        when(project.getTaskManager()).thenReturn(manager);
        when(manager.getStatisticsFolder()).thenReturn(statisticsFolder);
        TaskRegistry registry = mock(TaskRegistry.class, withSettings().mockMaker(MockMakers.INLINE));
        taskRegistry = mockStatic(TaskRegistry.class, withSettings().mockMaker(MockMakers.INLINE));
        taskRegistry.when(TaskRegistry::getInstance).thenReturn(registry);
    }

    @AfterEach
    public void tearDown() {
        if (taskRegistry != null) {
            taskRegistry.close();
        }
    }

    @Test
    public void storageBackedFlushDoesNotWriteMetadataOrPersistCachedSnapshots() {
        StorageTask task = task(storage);

        task.flushRuns(List.of(run("new-run")));

        assertFalse(Files.exists(metadataFile()));
        verifyNoInteractions(storage);
    }

    @Test
    public void storageBackedAddAndUpdateDoNotCreateMetadata() throws Exception {
        TaskImpl task = task(storage);
        TaskRunImpl run = run("new-run");

        addNewRun(task, run);
        assertFalse(Files.exists(metadataFile()));
        run.setRunDuration(42);
        updateRun(task, run);

        assertSame(run, task.getLastRun());
        assertFalse(Files.exists(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void noStorageStillWritesAndUpdatesFileMetadata() throws Exception {
        TaskImpl task = task(null);
        TaskRunImpl run = run("file-run");
        addNewRun(task, run);

        assertTrue(Files.isRegularFile(metadataFile()));
        DBTTaskRun loaded = task(null).getLastRun();
        assertNotNull(loaded);
        assertEquals(run.getId(), loaded.getId());
        assertFalse(loaded.isFinished());

        run.setRunDuration(42);
        updateRun(task, run);

        loaded = task(null).getLastRun();
        assertNotNull(loaded);
        assertEquals(42, loaded.getRunDuration());
        assertTrue(loaded.isFinished());
    }

    @Test
    public void legacyHistoryIsReadWithoutMigrationOrRewrite() throws Exception {
        byte[] metadata = writeLegacyHistory();
        TaskImpl task = task(storage);

        assertEquals(List.of("legacy-run"), runIds(task));
        DBTTaskRun legacy = task.getLastRun();
        assertNotNull(legacy);
        assertEquals("legacy-user", legacy.getStartUser());
        assertEquals(25, legacy.getRunDuration());
        task.refreshRunStatistics();

        assertEquals(List.of("legacy-run"), runIds(task));
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void newRunsDoNotGetAppendedToLegacyMetadata() throws Exception {
        byte[] metadata = writeLegacyHistory();
        TaskImpl task = task(storage);
        TaskRunImpl run = run("new-run");

        addNewRun(task, run);
        run.setRunDuration(42);
        updateRun(task, run);

        assertEquals(List.of("legacy-run", "new-run"), runIds(task));
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void mergesStoredAndLegacyRunsInTimeAndIdOrderAndRefreshesLastRun() throws Exception {
        byte[] metadata = writeLegacyHistory();
        DBTTaskRunRecord first = record("a-run", START_TIME, true);
        DBTTaskRunRecord second = record("b-run", START_TIME, true);
        DBTTaskRunRecord last = record("last-run", START_TIME + 1000, true);
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenReturn(List.of(last, second, first));
        TaskImpl task = task(storage);

        assertEquals(List.of("legacy-run", "a-run", "b-run", "last-run"), runIds(task));
        assertSame(last, task.getLastRun());
        verify(storage).findRuns(any(), eq(new DBTTaskRunStorage.Filter(
            PROJECT_ID, TASK_ID, null, null, null, null, null, DBTTaskRunStorage.Order.START_TIME, true
        )), eq(0), eq(100));

        DBTTaskRunRecord newest = record("newest-run", START_TIME + 2000, true);
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenReturn(List.of(newest, first, last, second));
        task.refreshRunStatistics();

        assertEquals(List.of("legacy-run", "a-run", "b-run", "last-run", "newest-run"), runIds(task));
        assertSame(newest, task.getLastRun());
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void storedSnapshotOverridesLegacyRunWithSameId() throws Exception {
        byte[] metadata = writeLegacyHistory();
        DBTTaskRunRecord stored = record("legacy-run", START_TIME, true);
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenReturn(List.of(stored));
        TaskImpl task = task(storage);

        assertArrayEquals(new DBTTaskRun[] {stored}, task.getAllRuns());
        assertSame(stored, task.getLastRun());
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void addNewRunReplacesAlreadyStoredRunningSnapshot() throws Exception {
        DBTTaskRunRecord snapshot = record("running-run", START_TIME, true);
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenReturn(List.of(snapshot));
        TaskImpl task = task(storage);
        TaskRunImpl liveRun = run(snapshot.getId());

        // addNewRun performs the initial read, after the recorder has already stored RUNNING.
        addNewRun(task, liveRun);

        assertEquals(1, task.getAllRuns().length);
        assertSame(liveRun, task.getAllRuns()[0]);
        assertSame(liveRun, task.getLastRun());
        assertFalse(Files.exists(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void updateReplacesStoredSnapshotByRunIdWithoutDuplicatingIt() throws Exception {
        DBTTaskRunRecord snapshot = record("running-run", START_TIME, true);
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenReturn(List.of(snapshot));
        TaskImpl task = task(storage);
        assertSame(snapshot, task.getLastRun());
        TaskRunImpl completed = run(snapshot.getId());
        completed.setRunDuration(42);

        updateRun(task, completed);

        assertEquals(1, task.getAllRuns().length);
        assertSame(completed, task.getAllRuns()[0]);
        assertSame(completed, task.getLastRun());
        assertTrue(task.getLastRun().isFinished());
        assertEquals(42, task.getLastRun().getRunDuration());
        assertFalse(Files.exists(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void storageReadFailureWithoutLegacyHistoryDoesNotCreateFallbackMetadata() throws Exception {
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenThrow(new DBException("History unavailable"));
        TaskImpl task = task(storage);
        assertNull(task.getLastRun());
        TaskRunImpl run = run("new-run");

        addNewRun(task, run);
        assertFalse(Files.exists(metadataFile()));
        run.setRunDuration(42);
        updateRun(task, run);

        assertSame(run, task.getLastRun());
        assertFalse(Files.exists(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void storageReadFailureKeepsLegacyHistoryAndDoesNotFallBackToWritingFiles() throws Exception {
        byte[] metadata = writeLegacyHistory();
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenThrow(new DBException("History unavailable"));
        TaskImpl task = task(storage);

        assertEquals(List.of("legacy-run"), runIds(task));
        TaskRunImpl run = run("new-run");
        addNewRun(task, run);
        run.setRunDuration(42);
        updateRun(task, run);

        assertSame(run, task.getLastRun());
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        verify(storage, never()).saveRun(any());
    }

    @Test
    public void failedDeletePreservesLogsLegacyMetadataAndCachedRuns() throws Exception {
        byte[] metadata = writeLegacyHistory();
        TaskImpl task = task(storage);
        DBTTaskRun legacy = task.getLastRun();
        assertNotNull(legacy);
        Path log = writeLog(legacy.getId());
        doThrow(new DBException("Delete unavailable")).when(storage).deleteRuns(PROJECT_ID, TASK_ID, legacy.getId());

        task.removeRun(legacy);

        verify(storage).deleteRuns(PROJECT_ID, TASK_ID, "legacy-run");
        assertEquals("existing log", Files.readString(log));
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        assertSame(legacy, task.getLastRun());
        assertEquals(List.of("legacy-run"), runIds(task));
        assertEquals(List.of("legacy-run"), runIds(task(null)));
    }

    @Test
    public void failedClearPreservesAllLogsLegacyMetadataAndCachedRuns() throws Exception {
        byte[] metadata = writeLegacyHistory();
        DBTTaskRunRecord stored = record("stored-run", START_TIME, true);
        when(storage.findRuns(any(), any(), anyInt(), anyInt())).thenReturn(List.of(stored));
        TaskImpl task = task(storage);
        DBTTaskRun[] before = task.getAllRuns();
        Path legacyLog = writeLog("legacy-run");
        Path storedLog = writeLog(stored.getId());
        doThrow(new DBException("Clear unavailable")).when(storage).deleteRuns(PROJECT_ID, TASK_ID, null);

        task.cleanRunStatistics();

        verify(storage).deleteRuns(PROJECT_ID, TASK_ID, null);
        assertEquals("existing log", Files.readString(legacyLog));
        assertEquals("existing log", Files.readString(storedLog));
        assertArrayEquals(metadata, Files.readAllBytes(metadataFile()));
        assertArrayEquals(before, task.getAllRuns());
        assertSame(stored, task.getLastRun());
        assertEquals(List.of("legacy-run"), runIds(task(null)));
    }

    @Test
    public void existingRunIdsResolveOriginalLogsAndLoglessRecordsCannotReadThem() throws Exception {
        TaskImpl task = task(storage);
        String runId = "202609281200-42";
        TaskRunImpl legacy = run(runId);
        DBTTaskRunRecord stored = record(runId, START_TIME, true);
        Path log = writeLog(runId);

        assertEquals(log, task.getRunLog(legacy));
        assertEquals(log, task.getRunLog(stored));
        try (var input = task.getRunLogInputStream(stored)) {
            assertEquals("existing log", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }

        DBTTaskRunRecord withoutLog = record(runId, START_TIME, false);
        assertNull(task.getRunLog(withoutLog));
        assertThrows(DBException.class, () -> task.getRunLogInputStream(withoutLog));
        assertEquals("existing log", Files.readString(log));
    }

    @NotNull
    private StorageTask task(@Nullable DBTTaskRunStorage runStorage) {
        return new StorageTask(project, taskType, runStorage);
    }

    @NotNull
    private TaskRunImpl run(@NotNull String id) {
        return new TestRun(id);
    }

    // OSGi loads this test and TaskImpl in different runtime packages despite their matching package names.
    private static void addNewRun(@NotNull TaskImpl task, @NotNull DBTTaskRun run) throws ReflectiveOperationException {
        var method = TaskImpl.class.getDeclaredMethod("addNewRun", DBTTaskRun.class);
        method.setAccessible(true);
        method.invoke(task, run);
    }

    private static void updateRun(@NotNull TaskImpl task, @NotNull TaskRunImpl run) throws ReflectiveOperationException {
        var method = TaskImpl.class.getDeclaredMethod("updateRun", TaskRunImpl.class);
        method.setAccessible(true);
        method.invoke(task, run);
    }

    @NotNull
    private DBTTaskRunRecord record(@NotNull String id, long startTime, boolean hasLog) {
        return new DBTTaskRunRecord(id, TASK_ID, "Task", "type", "Type", PROJECT_ID, "Project",
            startTime, 0, "user", "Test application", DBTTaskRunRecord.Status.RUNNING, hasLog);
    }

    @NotNull
    private List<String> runIds(@NotNull TaskImpl task) {
        return Arrays.stream(task.getAllRuns()).map(DBTTaskRun::getId).toList();
    }

    @NotNull
    private Path metadataFile() {
        return statisticsFolder.resolve(TASK_ID).resolve(TaskImpl.META_FILE_NAME);
    }

    @NotNull
    private byte[] writeLegacyHistory() throws IOException {
        Files.createDirectories(metadataFile().getParent());
        Files.writeString(metadataFile(), """
            {"runs":[{"id":"legacy-run","startTime":"202001010000","startUser":"legacy-user",
            "startedBy":"Legacy application","duration":25}]}
            """);
        return Files.readAllBytes(metadataFile());
    }

    @NotNull
    private Path writeLog(@NotNull String runId) throws IOException {
        Path folder = Files.createDirectories(statisticsFolder.resolve(TASK_ID));
        return Files.writeString(folder.resolve(TaskUtils.buildRunLogFileName(runId)), "existing log");
    }

    private static class TestRun extends TaskRunImpl {
        private TestRun(@NotNull String id) {
            super(id, new Date(START_TIME), "user", "Test application", null, null);
        }
    }

    private static class StorageTask extends TaskImpl {
        private final DBTTaskRunStorage storage;

        private StorageTask(
            @NotNull DBPProject project,
            @NotNull DBTTaskType type,
            @Nullable DBTTaskRunStorage storage
        ) {
            super(project, type, TASK_ID, "Task", null, new Date(START_TIME), null, null);
            this.storage = storage;
        }

        @Nullable
        @Override
        protected DBTTaskRunStorage getRunStorage() {
            return storage;
        }

        public void flushRuns(@NotNull List<? extends DBTTaskRun> runs) {
            super.flushRunStatistics(runs);
        }
    }
}
