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
import org.eclipse.core.runtime.OperationCanceledException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.dbeaver.model.task.DBTTaskRunRecord;
import org.jkiss.dbeaver.model.task.DBTTaskRunStorage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.StandardConstants;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class QMTaskExecutionTest {
    @Test
    public void snapshotsRemainIndependentAndFinishOnlyOnce() throws DBException {
        List<DBTTaskRunRecord> events = new ArrayList<>();
        DBTTask task = temporaryTask();
        DBTTaskRun run = managedRun();
        QMTaskExecution execution = execution(events, task, run);
        DBTTaskRunRecord started = events.getFirst();
        assertEquals(List.of(started), events);

        when(task.getId()).thenReturn("changed-task");
        when(task.getName()).thenReturn("Renamed task");
        when(task.getType().getId()).thenReturn("changed-type");
        when(task.getType().getName()).thenReturn("Renamed type");
        when(task.getProject().getId()).thenReturn("changed-project");
        when(task.getProject().getName()).thenReturn("Renamed project");
        when(run.getId()).thenReturn("changed-run");
        run.getStartTime().setTime(9999);
        when(run.getStartUser()).thenReturn("changed-user");
        when(run.getStartedBy()).thenReturn("changed-application");
        long beforeClose = System.currentTimeMillis();
        execution.close();
        execution.close();

        assertEquals(2, events.size());
        assertSame(started, events.getFirst());
        assertEquals(DBTTaskRunRecord.Status.RUNNING, started.status());
        assertEquals(0, started.finishTime());
        assertFalse(started.isFinished());
        assertEquals(-1, started.getRunDuration());
        DBTTaskRunRecord finished = events.getLast();
        assertNotSame(started, finished);
        assertEquals(DBTTaskRunRecord.Status.SUCCESS, finished.status());
        assertTrue(finished.isFinished());
        assertTrue(finished.isRunSuccess());
        assertTrue(finished.finishTime() >= beforeClose);
        assertTrue(finished.finishTime() <= System.currentTimeMillis());
        assertEquals(finished.finishTime() - started.startTime(), finished.getRunDuration());
        assertEquals(new DBTTaskRunRecord(
            "managed-run", "#temp", "Transfer data", "dataExport", "Export", "task-test", "Task test",
            1234, finished.finishTime(), "task-user", "Test application", DBTTaskRunRecord.Status.SUCCESS, true
        ), finished);
        started.getStartTime().setTime(0);
        assertEquals(1234, started.getStartTime().getTime());
        assertEquals(1234, finished.getStartTime().getTime());
    }

    @Test
    public void simultaneousTemporaryTasksHaveDistinctExecutionIds() throws DBException {
        // Only unsaved runs need the product title; isolate its platform lookup with an inline static mock.
        try (MockedStatic<GeneralUtils> utils = mockStatic(GeneralUtils.class, withSettings().mockMaker(MockMakers.INLINE))) {
            utils.when(GeneralUtils::getProductTitle).thenReturn("Test product");
            List<DBTTaskRunRecord> events = new ArrayList<>();
            DBTTask task = temporaryTask();
            long beforeStart = System.currentTimeMillis();
            DBTTaskRunRecord firstRun;
            DBTTaskRunRecord secondRun;
            try (QMTaskExecution first = execution(events, task, null);
                 QMTaskExecution second = execution(events, task, null)) {
                firstRun = events.get(0);
                secondRun = events.get(1);
                assertNotEquals(firstRun.id(), secondRun.id());
                assertFalse(firstRun.id().isBlank());
                assertEquals(firstRun.taskId(), secondRun.taskId());
                assertEquals(firstRun.projectId(), secondRun.projectId());
                for (DBTTaskRunRecord run : List.of(firstRun, secondRun)) {
                    assertTrue(run.startTime() >= beforeStart);
                    assertTrue(run.startTime() <= System.currentTimeMillis());
                    assertEquals(System.getProperty(StandardConstants.ENV_USER_NAME, ""), run.startUser());
                    assertEquals("Test product", run.startedBy());
                    assertFalse(run.hasLog());
                }
                assertEquals(List.of(firstRun, secondRun), events);
            }
            assertEquals(4, events.size());
            assertEquals(secondRun.id(), events.get(2).id());
            assertEquals(firstRun.id(), events.get(3).id());
            assertFalse(events.get(2).hasLog());
            assertFalse(events.get(3).hasLog());
        }
    }

    @Test
    public void managedRunKeepsIdentityAndLogAssociationForAnyTaskType() throws DBException {
        for (String typeId : List.of("dataExport", "script", "custom-task-type")) {
            DBTTask task = temporaryTask();
            when(task.getType().getId()).thenReturn(typeId);
            when(task.getType().getName()).thenReturn("Type " + typeId);
            List<DBTTaskRunRecord> events = new ArrayList<>();
            try (QMTaskExecution ignored = execution(events, task, managedRun())) {
                assertEquals(1, events.size());
            }
            assertEquals(2, events.size());
            for (DBTTaskRunRecord event : events) {
                assertEquals("managed-run", event.getId());
                assertEquals("#temp", event.taskId());
                assertEquals("task-test", event.projectId());
                assertEquals(typeId, event.taskTypeId());
                assertEquals("Type " + typeId, event.taskTypeName());
                assertEquals(1234, event.startTime());
                assertEquals("task-user", event.getStartUser());
                assertEquals("Test application", event.getStartedBy());
                assertTrue(event.hasLog());
            }
        }
    }

    @Test
    public void laterSuccessfulCallbackDoesNotEraseFailure() throws DBException {
        List<DBTTaskRunRecord> events = new ArrayList<>();
        try (QMTaskExecution execution = execution(events)) {
            execution.recordError(new IllegalStateException("transfer failed"));
            execution.recordError(null);
            execution.recordCancellation(false);
        }
        assertEquals(DBTTaskRunRecord.Status.FAILED, events.getLast().status());
    }

    @Test
    public void swallowedInterruptionStillRecordsCancellation() throws DBException {
        List<DBTTaskRunRecord> events = new ArrayList<>();
        try (QMTaskExecution execution = execution(events)) {
            DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(mock(DBRProgressMonitor.class));
            InterruptedException error = new InterruptedException();
            assertSame(error, assertThrows(InterruptedException.class, () -> execution.run(context, true, true, monitor -> {
                throw error;
            })));
            execution.recordError(null);
        }
        assertEquals(DBTTaskRunRecord.Status.CANCELED, events.getLast().status());
    }

    @Test
    public void callbackCancellationIsRecordedWithoutCanceledMonitor() throws DBException {
        for (Throwable cancellation : List.of(
            new InterruptedException(),
            new OperationCanceledException(),
            new DBException("Task interrupted", new InterruptedException()),
            new DBException("Task canceled", new OperationCanceledException())
        )) {
            List<DBTTaskRunRecord> events = new ArrayList<>();
            try (QMTaskExecution execution = execution(events)) {
                execution.recordError(cancellation);
                execution.recordError(null);
            }
            assertEquals(2, events.size());
            assertEquals(DBTTaskRunRecord.Status.CANCELED, events.getLast().status(), cancellation.toString());
        }
    }

    @Test
    public void wrappedWorkerCancellationIsRecordedAndRethrown() throws DBException {
        for (Throwable cancellation : List.of(new InterruptedException(), new OperationCanceledException())) {
            List<DBTTaskRunRecord> events = new ArrayList<>();
            try (QMTaskExecution execution = execution(events)) {
                DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(mock(DBRProgressMonitor.class));
                InvocationTargetException error = new InvocationTargetException(cancellation);
                assertSame(error, assertThrows(InvocationTargetException.class,
                    () -> execution.run(context, true, true, monitor -> {
                        throw error;
                    })));
            }
            assertEquals(2, events.size());
            assertEquals(DBTTaskRunRecord.Status.CANCELED, events.getLast().status(), cancellation.toString());
        }
    }

    @Test
    public void wrappedCallbackFailureIsNotCancellation() throws DBException {
        List<DBTTaskRunRecord> events = new ArrayList<>();
        try (QMTaskExecution execution = execution(events)) {
            execution.recordError(new DBException("Task failed", new IllegalStateException("worker failed")));
            execution.recordError(null);
        }
        assertEquals(DBTTaskRunRecord.Status.FAILED, events.getLast().status());
    }

    @Test
    public void cancellationSurvivesCleanupMonitorReset() throws Exception {
        List<DBTTaskRunRecord> events = new ArrayList<>();
        try (QMTaskExecution execution = execution(events)) {
            DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
            NullProgressMonitor nested = new NullProgressMonitor();
            when(monitor.getNestedMonitor()).thenReturn(nested);
            when(monitor.isCanceled()).thenAnswer(call -> nested.isCanceled());
            DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(monitor);
            execution.run(context, true, true, m -> m.getNestedMonitor().setCanceled(true));
            monitor.getNestedMonitor().setCanceled(false);
            execution.run(context, true, false, m -> {});
        }
        assertEquals(DBTTaskRunRecord.Status.CANCELED, events.getLast().status());
    }

    @Test
    public void swallowedWorkerFailureIsRecorded() throws DBException {
        List<DBTTaskRunRecord> events = new ArrayList<>();
        try (QMTaskExecution execution = execution(events)) {
            DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(mock(DBRProgressMonitor.class));
            InvocationTargetException error = new InvocationTargetException(new IllegalStateException("worker failed"));
            assertSame(error, assertThrows(InvocationTargetException.class, () -> execution.run(context, true, true, monitor -> {
                throw error;
            })));
        }
        assertEquals(DBTTaskRunRecord.Status.FAILED, events.getLast().status());
    }

    @Test
    public void uncheckedWorkerErrorsAreRecordedAndRethrown() throws DBException {
        for (Throwable error : List.of(new OperationCanceledException(), new IllegalStateException(), new AssertionError())) {
            List<DBTTaskRunRecord> events = new ArrayList<>();
            try (QMTaskExecution execution = execution(events)) {
                DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(mock(DBRProgressMonitor.class));
                assertSame(error, assertThrows(error.getClass(), () -> execution.run(context, true, true, monitor -> {
                    if (error instanceof Error fatal) {
                        throw fatal;
                    }
                    throw (RuntimeException) error;
                })));
            }
            assertEquals(error instanceof OperationCanceledException ? DBTTaskRunRecord.Status.CANCELED :
                DBTTaskRunRecord.Status.FAILED, events.getLast().status());
        }
    }

    @Test
    public void cancellationTakesPrecedenceOverFailureInEitherOrder() throws DBException {
        for (boolean cancelFirst : List.of(false, true)) {
            List<DBTTaskRunRecord> events = new ArrayList<>();
            try (QMTaskExecution execution = execution(events)) {
                execution.recordCancellation(cancelFirst);
                execution.recordError(new IllegalStateException("worker failed"));
                execution.recordCancellation(!cancelFirst);
                execution.recordCancellation(false);
            }
            assertEquals(DBTTaskRunRecord.Status.CANCELED, events.getLast().status());
        }
    }

    @Test
    public void publisherFailuresDoNotPreventExecutionOrRepeatedClose() throws Exception {
        List<DBTTaskRunRecord> attempts = new ArrayList<>();
        DBTTaskRunStorage storage = mock(DBTTaskRunStorage.class);
        doAnswer(call -> {
            attempts.add(call.getArgument(0));
            throw new DBException("History unavailable");
        }).when(storage).saveRun(any());
        try (MockedStatic<DBTTaskRunStorage> storageLookup = mockStatic(
            DBTTaskRunStorage.class, withSettings().mockMaker(MockMakers.INLINE)
        )) {
            storageLookup.when(DBTTaskRunStorage::getInstance).thenReturn(storage);
            QMTaskExecution execution = assertDoesNotThrow(() -> new QMTaskExecution(temporaryTask(), managedRun()));
            AtomicBoolean ran = new AtomicBoolean();
            DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(mock(DBRProgressMonitor.class));
            execution.run(context, true, true, monitor -> ran.set(true));
            assertTrue(ran.get());
            assertDoesNotThrow(execution::close);
            assertDoesNotThrow(execution::close);
        }
        assertEquals(2, attempts.size());
        assertEquals(DBTTaskRunRecord.Status.RUNNING, attempts.getFirst().status());
        assertEquals(DBTTaskRunRecord.Status.SUCCESS, attempts.getLast().status());
    }

    @Test
    public void snapshotsExcludeSettingsResultsAndLogContents() throws Exception {
        DBTTask task = temporaryTask();
        DBTTaskRun run = managedRun();
        String sensitive = "private-task-payload";
        when(task.getProperties()).thenReturn(Map.of("password", sensitive));
        when(run.getErrorMessage()).thenReturn(sensitive);
        when(run.getErrorStackTrace()).thenReturn(sensitive);
        when(run.getExtraMessage()).thenReturn(sensitive);
        List<DBTTaskRunRecord> events = new ArrayList<>();
        try (QMTaskExecution execution = execution(events, task, run)) {
            execution.recordError(new IllegalStateException(sensitive));
        }
        verify(task, never()).getProperties();
        verify(task, never()).getRunLog(any());
        verify(task, never()).getRunLogInputStream(any());
        verify(run, never()).getErrorMessage();
        verify(run, never()).getErrorStackTrace();
        verify(run, never()).getExtraMessage();
        assertEquals("Failed", events.getLast().getErrorMessage());
        for (DBTTaskRunRecord event : events) {
            assertNull(event.getErrorStackTrace());
            assertNull(event.getExtraMessage());
            assertFalse(event.toString().contains(sensitive));
        }
    }

    @NotNull
    private QMTaskExecution execution(@NotNull List<DBTTaskRunRecord> events) throws DBException {
        return execution(events, temporaryTask(), managedRun());
    }

    @NotNull
    private QMTaskExecution execution(
        @NotNull List<DBTTaskRunRecord> events,
        @NotNull DBTTask task,
        @Nullable DBTTaskRun run
    ) throws DBException {
        DBTTaskRunStorage storage = mock(DBTTaskRunStorage.class);
        doAnswer(call -> {
            events.add(call.getArgument(0));
            return null;
        }).when(storage).saveRun(any());
        try (MockedStatic<DBTTaskRunStorage> storageLookup = mockStatic(
            DBTTaskRunStorage.class, withSettings().mockMaker(MockMakers.INLINE)
        )) {
            storageLookup.when(DBTTaskRunStorage::getInstance).thenReturn(storage);
            // The public constructor captures storage; subsequent saves use the same mock after this scope closes.
            return run == null ? new QMTaskExecution(task) : new QMTaskExecution(task, run);
        }
    }

    @NotNull
    private DBTTaskRun managedRun() {
        DBTTaskRun run = mock(DBTTaskRun.class);
        when(run.getId()).thenReturn("managed-run");
        when(run.getStartTime()).thenReturn(new Date(1234));
        when(run.getStartUser()).thenReturn("task-user");
        when(run.getStartedBy()).thenReturn("Test application");
        return run;
    }

    @NotNull
    private DBTTask temporaryTask() {
        DBTTask task = mock(DBTTask.class, RETURNS_DEEP_STUBS);
        DBPProject project = task.getProject();
        when(project.getId()).thenReturn("task-test");
        when(project.getName()).thenReturn("Task test");
        when(task.getId()).thenReturn("#temp");
        when(task.getName()).thenReturn("Transfer data");
        when(task.getType().getId()).thenReturn("dataExport");
        when(task.getType().getName()).thenReturn("Export");
        return task;
    }
}
