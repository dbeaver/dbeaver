/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;

import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * SQLToolExecuteHandler
 */
public abstract class SQLToolExecuteHandler<OBJECT_TYPE extends DBSObject, SETTINGS extends SQLToolExecuteSettings<OBJECT_TYPE>> implements DBTTaskHandler {

    @Override
    public final void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull Writer logStream,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        SETTINGS settings = createToolSettings();
        settings.loadConfiguration(runnableContext, task.getProperties());
        executeWithSettings(runnableContext, task, locale, log, logStream, listener, settings);
    }

    private void executeWithSettings(@NotNull DBRRunnableContext runnableContext, DBTTask task, @NotNull Locale locale, @NotNull Log log, Writer logStream, @NotNull DBTTaskExecutionListener listener, SETTINGS settings) throws DBException {
        // Start consumers
        listener.taskStarted(settings);

        Throwable error = null;
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    executeTool(monitor, task, settings, log, logStream);
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
        listener.taskFinished(settings, error);
    }

    private void executeTool(DBRProgressMonitor monitor, DBTTask task, SETTINGS settings, Log log, Writer logStream) throws DBException {
        List<OBJECT_TYPE> objectList = settings.getObjectList();
        monitor.beginTask(task.getType().getName(), objectList.size());
        for (OBJECT_TYPE object : objectList) {
            monitor.subTask(DBUtils.getObjectFullName(object, DBPEvaluationContext.UI));
            try (DBCSession session = DBUtils.openUtilSession(monitor, object, "Execute " + task.getType().getName())) {
                List<DBEPersistAction> queries = new ArrayList<>();
                generateObjectQueries(session, settings, queries, object);

                DBExecUtils.executeScript(monitor, session.getExecutionContext(), task.getType().getName(), queries);
            }
            monitor.worked(1);
        }
        monitor.done();
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

}
