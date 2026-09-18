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
package org.jkiss.dbeaver.model.qm;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.qm.meta.QMMTaskInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class QMTaskExecutionTest extends DBeaverUnitTest {
    @BeforeEach
    public void initializeQueryManager() {
        if (QMUtils.getEventBrowser(true) == null) {
            QMUtils.initPlatform(false);
        }
    }

    @Test
    public void snapshotsRemainIndependentAndFinishOnlyOnce() {
        List<QMMTaskInfo> events = new ArrayList<>();
        try (Registration ignored = publisher(events)) {
            DBTTask task = temporaryTask();
            QMTaskExecution execution = new QMTaskExecution(task);
            when(task.getName()).thenReturn("Renamed task");
            execution.close();
            execution.close();

            assertEquals(2, events.size());
            assertEquals(QMMTaskInfo.Status.STARTED, events.get(0).getStatus());
            assertFalse(events.get(0).isClosed());
            assertEquals(QMMTaskInfo.Status.SUCCESS, events.get(1).getStatus());
            assertTrue(events.get(1).isClosed());
            assertEquals("Transfer data", events.get(1).getTaskName());
            assertEquals(events.get(0).getExecutionId(), events.get(1).getExecutionId());
            assertNotEquals(events.get(0).getObjectId(), events.get(1).getObjectId());
        }
    }

    @Test
    public void simultaneousTemporaryTasksHaveDistinctExecutionIds() {
        List<QMMTaskInfo> events = new ArrayList<>();
        try (Registration ignored = publisher(events)) {
            DBTTask task = temporaryTask();
            try (QMTaskExecution first = new QMTaskExecution(task); QMTaskExecution second = new QMTaskExecution(task)) {
                assertNotEquals(events.get(0).getExecutionId(), events.get(1).getExecutionId());
                assertEquals(events.get(0).getConnection().getContainerId(), events.get(1).getConnection().getContainerId());
            }
            assertEquals(4, events.size());
        }
    }

    @Test
    public void laterSuccessfulCallbackDoesNotEraseFailure() {
        List<QMMTaskInfo> events = new ArrayList<>();
        try (Registration ignored = publisher(events)) {
            try (QMTaskExecution execution = new QMTaskExecution(temporaryTask())) {
                execution.recordError(new IllegalStateException("transfer failed"));
                execution.recordError(null);
            }
            assertEquals(QMMTaskInfo.Status.FAILED, events.getLast().getStatus());
        }
    }

    @Test
    public void swallowedInterruptionStillRecordsCancellation() {
        List<QMMTaskInfo> events = new ArrayList<>();
        try (Registration ignored = publisher(events)) {
            try (QMTaskExecution execution = new QMTaskExecution(temporaryTask())) {
                DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(new VoidProgressMonitor());
                assertThrows(InterruptedException.class, () -> execution.run(context, true, true, monitor -> {
                    throw new InterruptedException();
                }));
                execution.recordError(null);
            }
            assertEquals(QMMTaskInfo.Status.CANCELED, events.getLast().getStatus());
        }
    }

    @Test
    public void cancellationSurvivesCleanupMonitorReset() throws Exception {
        List<QMMTaskInfo> events = new ArrayList<>();
        try (Registration ignored = publisher(events)) {
            try (QMTaskExecution execution = new QMTaskExecution(temporaryTask())) {
                DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
                NullProgressMonitor nested = new NullProgressMonitor();
                when(monitor.getNestedMonitor()).thenReturn(nested);
                when(monitor.isCanceled()).thenAnswer(call -> nested.isCanceled());
                DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(monitor);
                execution.run(context, true, true, m -> m.getNestedMonitor().setCanceled(true));
                monitor.getNestedMonitor().setCanceled(false);
                execution.run(context, true, false, m -> {});
            }
            assertEquals(QMMTaskInfo.Status.CANCELED, events.getLast().getStatus());
        }
    }

    @Test
    public void swallowedWorkerFailureIsRecorded() {
        List<QMMTaskInfo> events = new ArrayList<>();
        try (Registration ignored = publisher(events)) {
            try (QMTaskExecution execution = new QMTaskExecution(temporaryTask())) {
                DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(new VoidProgressMonitor());
                assertThrows(InvocationTargetException.class, () -> execution.run(context, true, true, monitor -> {
                    throw new InvocationTargetException(new IllegalStateException("worker failed"));
                }));
            }
            assertEquals(QMMTaskInfo.Status.FAILED, events.getLast().getStatus());
        }
    }

    private interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    private Registration publisher(List<QMMTaskInfo> events) {
        QMExecutionHandler handler = mock(QMExecutionHandler.class);
        doAnswer(call -> { events.add(call.getArgument(1)); return null; }).when(handler).handleTaskBegin(any(), any());
        doAnswer(call -> { events.add(call.getArgument(1)); return null; }).when(handler).handleTaskEnd(any(), any());
        QMUtils.registerHandler(handler);
        return () -> QMUtils.unregisterHandler(handler);
    }

    private DBTTask temporaryTask() {
        DBTTask task = mock(DBTTask.class, RETURNS_DEEP_STUBS);
        DBPProject project = task.getProject();
        when(project.getProjectID()).thenReturn(UUID.randomUUID());
        when(project.getId()).thenReturn("task-test");
        when(project.getName()).thenReturn("Task test");
        when(project.getAbsolutePath()).thenReturn(Path.of("task-test"));
        when(project.getWorkspace()).thenReturn(DBWorkbench.getPlatform().getWorkspace());
        when(task.getId()).thenReturn("#temp");
        when(task.getName()).thenReturn("Transfer data");
        when(task.getType().getId()).thenReturn("dataExport");
        when(task.getType().getName()).thenReturn("Export");
        return task;
    }
}
