/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Locale;

/**
 * TaskRunJob
 */
public class TaskRunJob extends AbstractJob implements DBRRunnableContext {

    private static final String RUN_LOG_PREFIX = "run_";
    private static final String RUN_LOG_EXT = "log";

    private static final Log log = Log.getLog(TaskRunJob.class);

    private final TaskImpl task;
    private final Locale locale;
    private DBTTaskExecutionListener executionListener;
    private Log taskLog = log;
    private DBRProgressMonitor activeMonitor;

    protected TaskRunJob(TaskImpl task, Locale locale, DBTTaskExecutionListener executionListener) {
        super("Task [" + task.getType().getName() + "] runner - " + task.getName());
        this.task = task;
        this.locale = locale;
        this.executionListener = executionListener;

    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        try {
            Date startTime = new Date();
            File taskStatsFolder = task.getTaskStatsFolder();
            if (!taskStatsFolder.exists() && !taskStatsFolder.mkdirs()) {
                throw new IOException("Can't create task log folder '" + taskStatsFolder.getAbsolutePath() + "'");
            }
            File logFile = new File(taskStatsFolder, RUN_LOG_PREFIX + TaskManagerImpl.systemDateFormat.format(startTime) + "." + RUN_LOG_EXT);
            try (OutputStream logStream = new FileOutputStream(logFile)) {
                taskLog = new Log(getName(), logStream);
                try {
                    executeTask(new LoggingProgressMonitor(monitor));
                } finally {
                    taskLog.flush();
                }
            }
        } catch (Exception e) {
            taskLog.error("Task fatal error", e);
            return GeneralUtils.makeExceptionStatus(e);
        }
        return Status.OK_STATUS;
    }

    private void executeTask(DBRProgressMonitor monitor) throws DBException {
        activeMonitor = monitor;
        DBTTaskHandler taskHandler = task.getType().createHandler();
        taskHandler.executeTask(this, task, locale, taskLog, executionListener);
    }

    @Override
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        runnable.run(activeMonitor);
    }

    private class LoggingProgressMonitor extends ProxyProgressMonitor {
        public LoggingProgressMonitor(DBRProgressMonitor monitor) {
            super(monitor);
        }

        @Override
        public void beginTask(String name, int totalWork) {
            super.beginTask(name, totalWork);
            taskLog.debug(">> " + name);
        }

        @Override
        public void subTask(String name) {
            super.subTask(name);
            taskLog.debug(">>> " + name);
        }
    }

}
