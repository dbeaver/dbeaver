/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskRunJob
 */
public class TaskRunJob extends AbstractJob implements DBRRunnableContext {

    private static final Log log = Log.getLog(TaskRunJob.class);

    private static final AtomicInteger taskNumber = new AtomicInteger(0);

    private final TaskImpl task;
    private final Locale locale;
    private final DBTTaskExecutionListener executionListener;
    private Log taskLog = log;
    private DBRProgressMonitor activeMonitor;
    private DBTTaskRunStatus taskRunStatus = new DBTTaskRunStatus();

    private long taskStartTime;
    private long elapsedTime;
    private Throwable taskError;

    private boolean canceledByTimeOut = false;

    public TaskRunJob(TaskImpl task, Locale locale, DBTTaskExecutionListener executionListener) {
        super("Task [" + task.getType().getName() + "] runner - " + task.getName());
        setUser(true);
        setSystem(false);
        this.task = task;
        this.locale = locale;
        this.executionListener = new LoggingExecutionListener(executionListener);

    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        Date startTime = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.getDefault()); //$NON-NLS-1$
        dateFormat.setTimeZone(TimeZone.getTimeZone(TimezoneRegistry.getUserDefaultTimezone()));
        String taskId = dateFormat.format(startTime) + "_" + taskNumber.incrementAndGet();
        TaskRunImpl taskRun = new TaskRunImpl(
            taskId,
            new Date(),
            System.getProperty(StandardConstants.ENV_USER_NAME),
            GeneralUtils.getProductTitle(),
            null, null);
        task.getTaskStatsFolder(true);
        Path logFile = Objects.requireNonNull(task.getRunLog(taskRun)); // must exist on local machine
        task.addNewRun(taskRun);

        try (PrintStream logStream = new PrintStream(Files.newOutputStream(logFile), true, StandardCharsets.UTF_8)) {
            taskLog = Log.getLog(TaskRunJob.class);
            Log.setLogWriter(logStream);
            taskLog.info(String.format("Task '%s' (%s) started", task.getName(), task.getId()));
            monitor.beginTask("Run task '" + task.getName() + " (" + task.getType().getName() + ")", 1);
            try {
                taskRunStatus = executeTask(new TaskLoggingProgressMonitor(monitor, task), logStream);
                taskRun.setExtraMessage(taskRunStatus.getResultMessage());
            } catch (Throwable e) {
                taskError = e;
                taskLog.error("Task fatal error", e);
            } finally {
                monitor.done();
             
                taskRun.setRunDuration(elapsedTime);
                if (activeMonitor.isCanceled() || monitor.isCanceled()) {
                    taskRun.setErrorMessage("Canceled");
                    taskLog.info(String.format("Task '%s' (%s) cancelled after %s ms", task.getName(), task.getId(), elapsedTime));
                } else if (taskError != null) {
                    String errorMessage = taskError.getMessage();
                    if (CommonUtils.isEmpty(errorMessage)) {
                        errorMessage = taskError.getClass().getName();
                    }
                    taskRun.setErrorMessage(errorMessage);
                    StringWriter buf = new StringWriter();
                    taskError.printStackTrace(new PrintWriter(buf, true));
                    taskRun.setErrorStackTrace(buf.toString());
                    taskLog.info(String.format("Task '%s' (%s) finished with errors in %s ms", task.getName(), task.getId(), elapsedTime));
                } else {
                    taskLog.info(String.format("Task '%s' (%s) finished successfully in %s ms", task.getName(), task.getId(), elapsedTime));
                }
                task.updateRun(taskRun);
                taskLog.flush();
                Log.setLogWriter(null);
            }
        } catch (IOException e) {
            log.error("Error opening task run log file", e);
        }
        return Status.OK_STATUS;
    }

    private DBTTaskRunStatus executeTask(DBRProgressMonitor monitor, PrintStream logWriter) throws DBException, InterruptedException {
        activeMonitor = monitor;
        DBTaskUtils.confirmTaskOrThrow(task, taskLog, logWriter);
        DBTTaskHandler taskHandler = task.getType().createHandler();
        DBTTaskRunStatus taskStatus = taskHandler.executeTask(this, task, locale, taskLog, logWriter, executionListener);
        if (monitor.isCanceled()) {
            if (canceledByTimeOut) {
                taskStatus.setResultMessage("by timeout reached");
            }
            if (taskStatus.getResultMessage() == null) {
                taskStatus.setResultMessage("by user");
            }
        }
        return taskStatus;
    }

    @Override
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        runnable.run(activeMonitor);
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
            taskStartTime = System.currentTimeMillis();
            parent.taskStarted(task);
        }

        @Override
        public void taskFinished(@Nullable DBTTask task, @Nullable Object result, @Nullable Throwable error, @Nullable Object settings) {
            parent.taskFinished(task, result, error, settings);
            elapsedTime = System.currentTimeMillis() - taskStartTime;
            taskError = error;
        }

        @Override
        public void subTaskFinished(@Nullable DBTTask task, @Nullable Throwable error, @Nullable Object settings) {
            parent.subTaskFinished(task, error, settings);
        }
    }

    /**
     * Cancel task by time reached
     */
    public void cancelByTimeReached() {
        if (task.getMaxExecutionTime() > 0
            && taskStartTime > 0
            && (System.currentTimeMillis() - taskStartTime) > (task.getMaxExecutionTime() * 1000L)) {
            canceledByTimeOut = true;
            cancel();
            activeMonitor.getNestedMonitor().setCanceled(true);
            if (isRunDirectly()) {
                canceling();
            }
        }
    }

}
