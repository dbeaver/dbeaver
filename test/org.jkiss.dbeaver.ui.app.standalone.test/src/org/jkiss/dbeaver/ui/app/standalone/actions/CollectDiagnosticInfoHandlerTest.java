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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.runtime.OperationCanceledException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectDiagnosticInfoHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void doesNotOpenProjectsWhileCollectingTaskLogs() {
        DBPProject closed = Mockito.mock(DBPProject.class);
        DBPProject open = Mockito.mock(DBPProject.class);
        Mockito.when(open.isOpen()).thenReturn(true);
        var entries = new ArrayList<CollectDiagnosticInfoHandler.DiagnosticsEntry>();
        CollectDiagnosticInfoHandler.addTaskLogs(entries, List.of(closed, open));

        assertTrue(entries.isEmpty());
        Mockito.verify(closed, Mockito.never()).getTaskManager(false);
        Mockito.verify(open).getTaskManager(false);
        Mockito.verify(closed, Mockito.never()).getTaskManager();
        Mockito.verify(open, Mockito.never()).getTaskManager();
    }

    @Test
    void includesTaskLogsFromAlreadyLoadedManagers() {
        DBPProject open = Mockito.mock(DBPProject.class);
        DBTTaskManager manager = Mockito.mock(DBTTaskManager.class);
        DBTTask task = Mockito.mock(DBTTask.class);
        DBTTaskRun run = Mockito.mock(DBTTaskRun.class);
        Path logPath = Path.of("task.log");
        Mockito.when(open.isOpen()).thenReturn(true);
        Mockito.when(open.getName()).thenReturn("project");
        Mockito.when(open.getTaskManager(false)).thenReturn(manager);
        Mockito.when(manager.getAllTasks()).thenReturn(new DBTTask[]{task});
        Mockito.when(task.getName()).thenReturn("task");
        Mockito.when(task.getAllRuns()).thenReturn(new DBTTaskRun[]{run});
        Mockito.when(task.getRunLog(run)).thenReturn(logPath);

        var entries = new ArrayList<CollectDiagnosticInfoHandler.DiagnosticsEntry>();
        CollectDiagnosticInfoHandler.addTaskLogs(entries, List.of(open));

        assertEquals(List.of(new CollectDiagnosticInfoHandler.DiagnosticsEntry(logPath, "tasks/project/task/task.log")), entries);
        Mockito.verify(open, Mockito.never()).getTaskManager();
    }

    @Test
    void progressAccountsForFileSizesAndEmptyFiles() throws IOException {
        Path large = Files.write(tempDir.resolve("large.log"), new byte[128 * 1024]);
        Path empty = Files.createFile(tempDir.resolve("empty.log"));
        var entries = List.of(
            new CollectDiagnosticInfoHandler.DiagnosticsEntry(large, "large.log"),
            new CollectDiagnosticInfoHandler.DiagnosticsEntry(empty, "empty.log")
        );
        var plan = CollectDiagnosticInfoHandler.planProgress(entries);
        assertEquals(4, plan.totalWork()); // One for configuration, two for the large log, one for the empty log.
        assertEquals(2, plan.entries().get(0).work());
        assertEquals(1, plan.entries().get(1).work());

        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        try (var input = new CollectDiagnosticInfoHandler.ProgressInputStream(
            Files.newInputStream(large), monitor, plan.bytesPerWork(), plan.entries().get(0).work())) {
            input.transferTo(OutputStream.nullOutputStream());
            input.complete();
        }
        Mockito.verify(monitor, Mockito.times(2)).worked(1);
    }

    @Test
    void cancelsWhileReadingALog() throws IOException {
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenReturn(false, true);
        try (var input = new CollectDiagnosticInfoHandler.ProgressInputStream(
            new ByteArrayInputStream(new byte[2]), monitor, 1, 2)) {
            assertEquals(0, input.read());
            assertThrows(OperationCanceledException.class, input::read);
        }
        Mockito.verify(monitor).worked(1);
    }
}
