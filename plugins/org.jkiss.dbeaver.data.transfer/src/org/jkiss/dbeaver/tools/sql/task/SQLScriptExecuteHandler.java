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
package org.jkiss.dbeaver.tools.sql.task;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.rm.RMControllerProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * SQLScriptExecuteHandler
 */
public class SQLScriptExecuteHandler implements DBTTaskHandler {

    private final DBCStatistics totalStatistics = new DBCStatistics();

    @Override
    @NotNull
    public DBTTaskRunStatus executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull PrintStream logStream,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        SQLScriptExecuteSettings settings = new SQLScriptExecuteSettings();
        settings.loadConfiguration(runnableContext, task);
        executeWithSettings(runnableContext, task, locale, log, logStream, listener, settings);
        return DBTTaskRunStatus.makeStatisticsStatus(totalStatistics);
    }

    private void executeWithSettings(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull PrintStream logStream,
        @NotNull DBTTaskExecutionListener listener,
        @NotNull SQLScriptExecuteSettings settings
    ) throws DBException {
        log.debug("SQL Scripts Execute");

        // Start consumers
        listener.taskStarted(task);
        Throwable error = null;
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    runScripts(monitor, task, settings, log, logStream);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            log.debug("Task canceled");
        }
        if (error != null) {
            log.error(error);
        }
        listener.taskFinished(task, null, error, settings);

        log.debug("SQL script execute completed");
    }

    private void runScripts(DBRProgressMonitor monitor, DBTTask task, SQLScriptExecuteSettings settings, Log log, PrintStream logStream) throws DBException {
        List<DBPDataSourceContainer> dataSources = settings.getDataSources();

        for (String filePath : settings.getScriptFiles()) {
            try {
                for (DBPDataSourceContainer dataSourceContainer : dataSources) {
                    var sqlScriptContent = readScriptContents(monitor, task.getProject(), filePath);
                    if (!dataSourceContainer.isConnected()) {
                        dataSourceContainer.connect(monitor, true, true);
                    }
                    DBPDataSource dataSource = dataSourceContainer.getDataSource();
                    if (dataSource == null) {
                        throw new DBException("Can't obtain data source connection");
                    }
                    DBCExecutionContext executionContext = dataSource.getDefaultInstance().getDefaultContext(monitor, false);

                    log.debug("> Execute script [" + filePath + "] in [" + dataSourceContainer.getName() + "]");
                    DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
                    if (contextDefaults != null) {
                        DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                        if (defaultCatalog != null) {
                            log.debug("> Default catalog: " + defaultCatalog.getName());
                        }
                        DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                        if (defaultSchema != null) {
                            log.debug("> Default schema: " + defaultSchema.getName());
                        }
                    }

                    processScript(monitor, task, settings, executionContext, filePath, sqlScriptContent, log, logStream);
                }
            } catch (Throwable e) {
                Throwable error = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
                throw new DBException("Error executing script '" + filePath + "'", error);
            }
        }
    }

    private void processScript(DBRProgressMonitor monitor, DBTTask task, SQLScriptExecuteSettings settings, DBCExecutionContext executionContext, String filePath, String sqlScriptContent, Log log, PrintStream logStream) throws DBException {
        PrintWriter logWriter = new PrintWriter(logStream, true);
        List<SQLScriptElement> scriptElements = SQLScriptParser.parseScript(executionContext.getDataSource(), sqlScriptContent);
        SQLScriptContext scriptContext = new SQLScriptContext(null, () -> executionContext, null, logWriter, null);
        scriptContext.setVariables(DBTaskUtils.getVariables(task));
        SQLScriptDataReceiver dataReceiver = new SQLScriptDataReceiver();
        SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(executionContext, scriptElements, scriptContext, dataReceiver, log);

        scriptProcessor.setCommitType(settings.isAutoCommit() ? SQLScriptCommitType.AUTOCOMMIT : SQLScriptCommitType.AT_END);
        scriptProcessor.setErrorHandling(settings.isIgnoreErrors() ? SQLScriptErrorHandling.IGNORE : SQLScriptErrorHandling.STOP_ROLLBACK);
        if (settings.isDumpQueryResultsToLog()) {
            dataReceiver.setDumpWriter(logWriter);
        }

        scriptProcessor.runScript(monitor);

        totalStatistics.accumulate(scriptProcessor.getTotalStatistics());
    }

    public static String readScriptContents(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPProject project,
        @NotNull String filePath
    ) throws DBException, IOException {
        java.nio.file.Path nioPath = DBFUtils.resolvePathFromString(monitor, project, filePath);
        if (!IOUtils.isLocalPath(nioPath)) {
            // Remote file
            return Files.readString(nioPath);
        }

        RMControllerProvider rmControllerProvider = DBUtils.getAdapter(RMControllerProvider.class, project);
        if (rmControllerProvider != null) {
            var rmController = rmControllerProvider.getResourceController();
            return new String(rmController.getResourceContents(project.getId(), filePath), StandardCharsets.UTF_8);
        }
        var sqlFile = DTUtils.findProjectFile(project, filePath);
        if (sqlFile == null) {
            throw new DBException("File " + filePath + " is not found in project " + project.getId());
        }
        try (Reader fileReader = Files.newBufferedReader(sqlFile)) {
            return IOUtils.readToString(fileReader);
        }
    }

}
