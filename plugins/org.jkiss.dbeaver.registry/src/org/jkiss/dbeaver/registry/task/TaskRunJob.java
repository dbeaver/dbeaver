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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.qm.QMTaskExecution;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TaskRunJob
 */
public class TaskRunJob extends AbstractJob implements DBRRunnableContext {

    private static final Log log = Log.getLog(TaskRunJob.class);

    private static final long PROCESS_ID = ProcessHandle.current().pid();

    private final TaskImpl task;
    private final Locale locale;
    private final DBTTaskExecutionListener executionListener;
    private Log taskLog = log;
    private DBRProgressMonitor activeMonitor;
    private DBTTaskRunStatus taskRunStatus = new DBTTaskRunStatus();

    private Instant taskStartTime = Instant.now();
    private Duration elapsedTime = Duration.ZERO;
    private Throwable taskError;
    private QMTaskExecution executionHistory;
    private volatile String runId;

    private final AtomicBoolean canceledByTimeOut = new AtomicBoolean();

    public TaskRunJob(TaskImpl task, Locale locale, DBTTaskExecutionListener executionListener) {
        super("Task [" + task.getType().getName() + "] runner - " + task.getName());
        setUser(true);
        setSystem(false);
        this.task = task;
        this.locale = locale;
        this.executionListener = new LoggingExecutionListener(executionListener);

    }

    @Override
    public void run(boolean fork, boolean cancelable, @NotNull DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException {
        if (executionHistory == null) {
            runnable.run(activeMonitor);
        } else {
            executionHistory.run((f, c, operation) -> operation.run(activeMonitor), fork, cancelable, runnable);
        }
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        Date startTime = new Date();
        taskStartTime = startTime.toInstant();
        runId = UUID.randomUUID().toString();
        TaskRunImpl taskRun = new TaskRunImpl(
            runId,
            startTime,
            System.getProperty(StandardConstants.ENV_USER_NAME),
            GeneralUtils.getProductTitle(),
            null, null);
        activeMonitor = new TaskLoggingProgressMonitor(monitor, task);
        executionHistory = new QMTaskExecution(task, taskRun);
        try {
            task.getTaskStatsFolder(true);
            Path logFile = Objects.requireNonNull(task.getRunLog(taskRun)); // must exist on local machine
            task.addNewRun(taskRun);
            try (PrintStream logStream = new PrintStream(Files.newOutputStream(logFile), true, StandardCharsets.UTF_8)) {
                taskLog = Log.getLog(TaskRunJob.class);
                PrintStream oldLogWriter = Log.getLogWriter();
                Log.setLogWriter(logStream);
                try {
                    taskLog.info(getExecutionDescription() + " started (mode=" + (isRunDirectly() ? "direct" : "background") + ")");
                    monitor.beginTask("Run task '" + task.getName() + " (" + task.getType().getName() + ")", 1);
                    try {
                        taskRunStatus = executeTask(activeMonitor, logStream);
                        taskRun.setExtraMessage(taskRunStatus.getResultMessage());
                    } catch (Throwable e) {
                        taskError = e;
                        executionHistory.recordError(e);
                        taskLog.error(getExecutionDescription() + " execution failed", e);
                    } finally {
                        monitor.done();
                        elapsedTime = getElapsedTime();
                        taskRun.setRunDuration(elapsedTime.toMillis());
                        if (activeMonitor.isCanceled() || monitor.isCanceled()) {
                            taskRun.setErrorMessage("Canceled");
                            taskLog.info(getExecutionDescription() + " canceled after " + elapsedTime.toMillis() + " ms"
                                + (canceledByTimeOut.get() ? " (timeout)" : ""));
                        } else if (taskError != null) {
                            String errorMessage = taskError.getMessage();
                            if (CommonUtils.isEmpty(errorMessage)) {
                                errorMessage = taskError.getClass().getName();
                            }
                            taskRun.setErrorMessage(errorMessage);
                            StringWriter buf = new StringWriter();
                            taskError.printStackTrace(new PrintWriter(buf, true));
                            taskRun.setErrorStackTrace(buf.toString());
                            taskLog.info(getExecutionDescription() + " finished with errors in " + elapsedTime.toMillis() + " ms");
                        } else {
                            taskLog.info(getExecutionDescription() + " finished successfully in " + elapsedTime.toMillis() + " ms");
                        }
                        taskLog.flush();
                    }
                } finally {
                    Log.setLogWriter(oldLogWriter);
                }
            }
        } catch (Exception e) {
            taskError = e;
            executionHistory.recordError(e);
            taskRun.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            log.error("Error preparing run log for " + getExecutionDescription(), e);
        } finally {
            executionHistory.recordCancellation(monitor.isCanceled() || activeMonitor.isCanceled());
            executionHistory.close();
            executionHistory = null;
            taskRun.setRunDuration(getElapsedTime().toMillis());
            task.updateRun(taskRun);
        }
        return Status.OK_STATUS;
    }

    private DBTTaskRunStatus executeTask(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PrintStream logWriter
    ) throws DBException, InterruptedException {
        activeMonitor = monitor;
        DBTaskUtils.confirmTaskOrThrow(task, taskLog, logWriter);
        DBTTaskHandler taskHandler = task.getType().createHandler();
        DBTTaskRunStatus taskStatus = taskHandler.executeTask(this, task, locale, taskLog, logWriter, executionListener);
        if (monitor.isCanceled()) {
            if (canceledByTimeOut.get()) {
                taskStatus.setResultMessage("by timeout reached");
            }
            if (taskStatus.getResultMessage() == null) {
                taskStatus.setResultMessage("by user");
            }
        }
        return taskStatus;
    }

    @NotNull
    public DBTTaskRunStatus getTaskRunStatus() {
        return taskRunStatus;
    }

    @Nullable
    public Throwable getTaskError() {
        return taskError;
    }

    private class LoggingExecutionListener implements DBTTaskExecutionListener {

        DBTTaskExecutionListener parent;

        public LoggingExecutionListener(DBTTaskExecutionListener src) {
            this.parent = src;
        }

        @Override
        public void taskStarted(@Nullable DBTTask task) {
            parent.taskStarted(task);
        }

        @Override
        public void taskFinished(@Nullable DBTTask task, @Nullable Object result, @Nullable Throwable error, @Nullable Object settings) {
            if (executionHistory != null) {
                executionHistory.recordError(error);
            }
            parent.taskFinished(task, result, error, settings);
            elapsedTime = getElapsedTime();
            taskError = error;
        }

        @Override
        public void subTaskFinished(@Nullable DBTTask task, @Nullable Throwable error, @Nullable Object settings) {
            parent.subTaskFinished(task, error, settings);
        }
    }

    public void cancelByTimeout() {
        if (canceledByTimeOut.compareAndSet(false, true)) {
            taskLog.info("Timeout cancellation requested for " + getExecutionDescription()
                + " [elapsedMs=" + getElapsedTime().toMillis() + ", limitMs=" + task.getMaxExecutionTime().toMillis() + "]");
        }
        cancel();
        activeMonitor.getNestedMonitor().setCanceled(true);
        if (isRunDirectly()) {
            canceling();
        }
    }

    @NotNull
    String getExecutionDescription() {
        return "Task '" + task.getName() + "' [project=" + task.getProject().getId()
            + ", task=" + task.getId() + ", type=" + task.getType().getId()
            + ", run=" + (runId == null ? "pending" : runId) + ", pid=" + PROCESS_ID + "]";
    }

    @NotNull
    public Duration getElapsedTime() {
        return Duration.between(taskStartTime, Instant.now());
    }

    @NotNull
    public TaskImpl getTask() {
        return task;
    }
}
