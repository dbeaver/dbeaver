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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TaskExecutionLogTest extends DBeaverUnitTest {
    @TempDir
    Path folder;

    private DBTTask task;
    private final DBRRunnableContext context = (fork, cancelable, runnable) -> runnable.run(new VoidProgressMonitor());

    @BeforeEach
    public void setUp() {
        task = Mockito.mock(DBTTask.class);
        Mockito.when(task.getName()).thenReturn("Unsaved export");
        Mockito.when(task.getRunLog(Mockito.any())).thenAnswer(invocation -> {
            DBTTaskRun run = invocation.getArgument(0);
            return folder.resolve("#temp").resolve(TaskUtils.buildRunLogFileName(run.getId()));
        });
    }

    @Test
    public void capturesUtf8OutputAndProgressInUniqueRunLogs() throws Exception {
        Path firstPath;
        Path secondPath;
        try (TaskExecutionLog first = TaskExecutionLog.open(task, context);
             TaskExecutionLog second = TaskExecutionLog.open(task, context)) {
            Assertions.assertNotNull(first);
            Assertions.assertNotNull(second);
            Assertions.assertNotEquals(first.getRun().getId(), second.getRun().getId());
            firstPath = task.getRunLog(first.getRun());
            secondPath = task.getRunLog(second.getRun());
            first.run(true, true, monitor -> {
                Assertions.assertSame(first.getLogStream(), Log.getLogWriter());
                monitor.beginTask("Export résumé", 1);
                Log.getLog(getClass()).info("First run output");
            });
            second.getLogStream().println("Second run output");
        }
        String firstLog = Files.readString(firstPath);
        String secondLog = Files.readString(secondPath);
        Assertions.assertTrue(firstLog.contains("Export résumé"));
        Assertions.assertTrue(firstLog.contains("First run output"));
        Assertions.assertFalse(firstLog.contains("Second run output"));
        Assertions.assertTrue(secondLog.contains("Second run output"));
        Assertions.assertFalse(secondLog.contains("First run output"));
    }

    @Test
    public void restoresPreviousWriterAfterSuccessFailureAndCancellation() throws Exception {
        PrintStream original = Log.getLogWriter();
        try (PrintStream previous = new PrintStream(new ByteArrayOutputStream());
             TaskExecutionLog executionLog = TaskExecutionLog.open(task, context)) {
            Assertions.assertNotNull(executionLog);
            Log.setLogWriter(previous);
            executionLog.run(false, true, monitor -> Log.getLog(getClass()).info("Success"));
            Assertions.assertSame(previous, Log.getLogWriter());
            Assertions.assertThrows(InvocationTargetException.class, () -> executionLog.run(false, true, monitor -> {
                throw new InvocationTargetException(new IllegalStateException("Transfer failed"));
            }));
            Assertions.assertSame(previous, Log.getLogWriter());
            Assertions.assertThrows(InterruptedException.class, () -> executionLog.run(false, true, monitor -> {
                throw new InterruptedException("Canceled");
            }));
            Assertions.assertSame(previous, Log.getLogWriter());
            Assertions.assertTrue(Files.readString(task.getRunLog(executionLog.getRun())).contains("Transfer failed"));
        } finally {
            Log.setLogWriter(original);
        }
    }

    @Test
    public void skipsCaptureWhenTaskHasNoLocalLogPath() {
        Mockito.when(task.getRunLog(Mockito.any())).thenReturn(null);
        Assertions.assertNull(TaskExecutionLog.open(task, context));
    }

    @Test
    public void logCreationFailureDoesNotPreventExecution() throws Exception {
        Path file = Files.createFile(folder.resolve("not-a-directory"));
        Mockito.when(task.getRunLog(Mockito.any())).thenReturn(file.resolve("run.log"));
        Assertions.assertNull(TaskExecutionLog.open(task, context));
    }
}
