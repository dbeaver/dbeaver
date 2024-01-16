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
package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.PrintStreamProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTTaskRunStatus;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * SQLToolExecuteHandler
 */
public abstract class SQLToolExecuteHandler<OBJECT_TYPE extends DBSObject, SETTINGS extends SQLToolExecuteSettings<OBJECT_TYPE>> implements DBTTaskHandler {

    private final DBCStatistics statistics = new DBCStatistics();

    @Override
    @NotNull
    public final DBTTaskRunStatus executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull PrintStream logStream,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        SETTINGS settings = createToolSettings();
        settings.loadConfiguration(runnableContext, task.getProperties(), task.getProject());
        executeWithSettings(runnableContext, task, locale, log, logStream, listener, settings);
        return DBTTaskRunStatus.makeStatisticsStatus(statistics);
    }

    private void executeWithSettings(@NotNull DBRRunnableContext runnableContext, DBTTask task, @NotNull Locale locale, @NotNull Log log, PrintStream logStream, @NotNull DBTTaskExecutionListener listener, SETTINGS settings) throws DBException {
        Throwable error = null;
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    executeTool(new PrintStreamProgressMonitor(monitor, logStream), task, settings, log, logStream, listener);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            log.debug("SQL tools canceled");
        }
        if (error != null) {
            log.error(error);
        }
    }

    private void executeTool(DBRProgressMonitor monitor, DBTTask task, SETTINGS settings, Log log, PrintStream outLog, DBTTaskExecutionListener listener) throws DBException, IOException {
        List<OBJECT_TYPE> objectList = settings.getObjectList();
        Exception lastError = null;

        listener.taskStarted(task);
        try {
            monitor.beginTask("Execute tool '" + task.getType().getName() + "'", objectList.size());
            List<Throwable> warnings = settings.getWarnings();
            if (!warnings.isEmpty()) {
                Throwable throwable = warnings.get(0);
                throw new DBCException("Tool execution error: " + throwable.getMessage(), throwable);
            }
            for (OBJECT_TYPE object : objectList) {
                monitor.subTask("Process [" + DBUtils.getObjectFullName(object, DBPEvaluationContext.UI) + "]");
                try (DBCSession session = DBUtils.openUtilSession(monitor, object, "Execute " + task.getType().getName())) {
                    List<DBEPersistAction> queries = new ArrayList<>();
                    generateObjectQueries(session, settings, queries, object);

                    DBCExecutionContext context = session.getExecutionContext();
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                    boolean isAutoCommitModeSwitchedOn = true;

                    try {
                        if(isRunInAutoCommit() && txnManager != null && !txnManager.isAutoCommit()){
                            isAutoCommitModeSwitchedOn = false;
                            txnManager.setAutoCommit(monitor, true);
                        }

                        for (DBEPersistAction action : queries) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            if (!CommonUtils.isEmpty(action.getTitle())) {
                                monitor.subTask(action.getTitle());
                            }
                            try {
                                if (action instanceof SQLDatabasePersistActionComment) {
                                    continue;
                                }
                                String script = action.getScript();
                                if (!CommonUtils.isEmpty(script)) {
                                    try (final DBCStatement statement = session.prepareStatement(
                                        DBCStatementType.SCRIPT,
                                        script,
                                        false,
                                        false,
                                        false
                                    )) {
                                        long startTime = System.currentTimeMillis();
                                        statement.executeStatement();
                                        long execTime = System.currentTimeMillis() - startTime;
                                        if (listener instanceof SQLToolRunListener) {
                                            if (action.getType() != DBEPersistAction.ActionType.INITIALIZER && action.getType() != DBEPersistAction.ActionType.FINALIZER) {
                                                SQLToolStatisticsSimple statisticsSimple = new SQLToolStatisticsSimple(object, false);
                                                if (SQLToolExecuteHandler.this instanceof SQLToolRunStatisticsGenerator) {
                                                    List<? extends SQLToolStatistics> executeStatistics =
                                                            ((SQLToolRunStatisticsGenerator) SQLToolExecuteHandler.this).getExecuteStatistics(
                                                                    object,
                                                                    settings,
                                                                    action,
                                                                    session,
                                                                    statement);
                                                    monitor.subTask("\tFinished in " + RuntimeUtils.formatExecutionTime(execTime));
                                                    if (!CommonUtils.isEmpty(executeStatistics)) {
                                                        for (SQLToolStatistics stat : executeStatistics) {
                                                            stat.setExecutionTime(execTime);
                                                        }
                                                        ((SQLToolRunListener) listener).handleActionStatistics(object, action, session, executeStatistics);
                                                    } else {
                                                        ((SQLToolRunListener) listener).handleActionStatistics(object, action, session, Collections.singletonList(statisticsSimple));
                                                    }
                                                } else {
                                                    ((SQLToolRunListener) listener).handleActionStatistics(object, action, session, Collections.singletonList(statisticsSimple));
                                                }
                                            }
                                        }

                                        statistics.addStatementsCount();
                                        statistics.addExecuteTime(execTime);
                                        try {
                                            long updateCount = statement.getUpdateRowCount();
                                            if (updateCount >= 0) {
                                                statistics.addRowsUpdated(updateCount);
                                            }
                                        } catch (DBCException e) {
                                            // In some cases we can't read update count
                                            // This is bad but we can live with it
                                            // Just print a warning
                                            log.warn("Can't obtain update count", e);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                lastError = e;
                                log.debug("Error executing query", e);
                                outLog.println("Error executing query\n" + e.getMessage());
                                if(listener instanceof SQLToolRunListener) {
                                    SQLToolStatisticsSimple errorStat = new SQLToolStatisticsSimple(object, true);
                                    errorStat.setStatusMessage(e.getMessage());
                                    ((SQLToolRunListener) listener).handleActionStatistics(object, action, session, Collections.singletonList(errorStat));
                                }
                            } finally {
                                monitor.worked(1);
                            }
                        }
                    } finally {
                        if (!isAutoCommitModeSwitchedOn) {
                            try {
                                txnManager.setAutoCommit(monitor, false);
                            } catch (DBCException e) {
                                log.debug("Cannot set auto-commit status", e);
                            }
                        }
                    }
                }
                monitor.worked(1);
            }
        } catch (Exception e) {
            lastError = e;
            outLog.println("Process error\n" + e.getMessage());
        } finally {
            monitor.done();
        }
        listener.taskFinished(task, null, lastError, settings);

        outLog.println("Tool execution finished");
        outLog.flush();
    }

    public String generateScript(DBRProgressMonitor monitor, SETTINGS settings) throws DBCException {
        List<DBEPersistAction> queries = new ArrayList<>();

        List<OBJECT_TYPE> objectList = settings.getObjectList();
        for (OBJECT_TYPE object : objectList) {
            try (DBCSession session = DBUtils.openUtilSession(monitor, object, "Generate tool queries")) {
                generateObjectQueries(session, settings, queries, object);
            }
        }

        String script = queries.stream().map(DBEPersistAction::getScript).collect(Collectors.joining(";\n"));
        script = script.trim();
        if (!script.isEmpty()) {
            // Add trailing delimiter (join doesn't do it)
            script += ";\n";
        }
        return script;
    }

    @NotNull
    public abstract SETTINGS createToolSettings();

    public abstract void generateObjectQueries(DBCSession session, SETTINGS settings, List<DBEPersistAction> queries, OBJECT_TYPE object) throws DBCException;

    public boolean isRunInSeparateTransaction() {
        return false;
    }

    public boolean isRunInAutoCommit() {
        return false;
    }

    public boolean isNeedConfirmation() {
        return false;
    }

    public boolean isOpenTargetObjectsOnFinish() {
        return false;
    }

    public boolean needsRefreshOnFinish() {
        return false;
    }

}
