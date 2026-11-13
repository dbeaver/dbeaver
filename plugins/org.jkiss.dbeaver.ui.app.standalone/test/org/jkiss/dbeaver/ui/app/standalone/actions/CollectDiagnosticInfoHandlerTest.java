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

import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectDiagnosticInfoHandlerTest {
    @Test
    void collectsRemainingLogsWhenTaskHistoryIsUnavailable() {
        DBPProject project = mock(DBPProject.class);
        DBTTaskManager manager = mock(DBTTaskManager.class);
        DBTTask unavailableTask = mock(DBTTask.class);
        DBTTask availableTask = mock(DBTTask.class);
        DBTTaskRun run = mock(DBTTaskRun.class);
        when(project.isOpen()).thenReturn(true);
        when(project.getName()).thenReturn("project");
        when(project.getTaskManager(false)).thenReturn(manager);
        when(manager.getAllTasks()).thenReturn(new DBTTask[] {unavailableTask, availableTask});
        when(unavailableTask.getName()).thenReturn("unavailable");
        when(unavailableTask.getAllRuns()).thenThrow(new NullPointerException("Task history is unavailable"));
        when(availableTask.getName()).thenReturn("available");
        when(availableTask.getAllRuns()).thenReturn(new DBTTaskRun[] {run});
        Path runLog = Path.of("run.log");
        when(availableTask.getRunLog(run)).thenReturn(runLog);
        var eclipseLog = new CollectDiagnosticInfoHandler.DiagnosticsEntry(Path.of(".log"), ".log");
        var entries = new ArrayList<>(List.of(eclipseLog));

        CollectDiagnosticInfoHandler.addTaskLogs(entries, List.of(project));

        assertEquals(List.of(
            eclipseLog,
            new CollectDiagnosticInfoHandler.DiagnosticsEntry(runLog, "tasks/project/available/run.log")
        ), entries);
    }
}
