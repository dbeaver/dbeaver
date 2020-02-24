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
package org.jkiss.dbeaver.tools.sql.task;

import org.eclipse.core.resources.IFile;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
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
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTaskUtils;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.utils.IOUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;

/**
 * SQLScriptExecuteHandler
 */
public class SQLScriptExecuteHandler implements DBTTaskHandler {

    @Override
    public void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull Writer logStream,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        SQLScriptExecuteSettings settings = new SQLScriptExecuteSettings();
        settings.loadConfiguration(runnableContext, task.getProperties());
        executeWithSettings(runnableContext, task, locale, log, logStream, listener, settings);
    }

    private void executeWithSettings(@NotNull DBRRunnableContext runnableContext, DBTTask task, @NotNull Locale locale, @NotNull Log log, Writer logStream, @NotNull DBTTaskExecutionListener listener, SQLScriptExecuteSettings settings) throws DBException {
        log.debug("SQL Scripts Execute");

        // Start consumers
        listener.taskStarted(settings);

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
        listener.taskFinished(settings, error);

        log.debug("SQL script execute completed");
    }

    private void runScripts(DBRProgressMonitor monitor, DBTTask task, SQLScriptExecuteSettings settings, Log log, Writer logStream) throws DBException {
        List<DBPDataSourceContainer> dataSources = settings.getDataSources();

        for (String filePath : settings.getScriptFiles()) {
            IFile sqlFile = SQLScriptExecuteSettings.getWorkspaceFile(filePath);
            try (InputStream sqlStream = sqlFile.getContents(true)) {
                try (Reader fileReader = new InputStreamReader(sqlStream, sqlFile.getCharset())) {
                    String sqlScriptContent = IOUtils.readToString(fileReader);
                    try {
                        for (DBPDataSourceContainer dataSourceContainer : dataSources) {
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
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            } catch (Throwable e) {
                Throwable error = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
                throw new DBException("Error executing script '" + filePath + "'", error);
            }
        }
    }

    private void processScript(DBRProgressMonitor monitor, DBTTask task, SQLScriptExecuteSettings settings, DBCExecutionContext executionContext, String filePath, String sqlScriptContent, Log log, Writer logStream) throws DBException {
        List<SQLScriptElement> scriptElements = SQLScriptParser.parseScript(executionContext, sqlScriptContent);
        SQLScriptContext scriptContext = new SQLScriptContext(null, () -> executionContext, null, logStream, null);
        scriptContext.setVariables(DBTaskUtils.getVariables(task));
        SQLScriptDataReceiver dataReceiver = new SQLScriptDataReceiver();
        SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(executionContext, scriptElements, scriptContext, dataReceiver, log);

        scriptProcessor.setCommitType(settings.isAutoCommit() ? SQLScriptCommitType.AUTOCOMMIT : SQLScriptCommitType.AT_END);
        scriptProcessor.setErrorHandling(settings.isIgnoreErrors() ? SQLScriptErrorHandling.IGNORE : SQLScriptErrorHandling.STOP_ROLLBACK);
        if (settings.isDumpQueryResultsToLog()) {
            dataReceiver.setDumpWriter(logStream);
        }

        scriptProcessor.runScript(monitor);
    }

}
