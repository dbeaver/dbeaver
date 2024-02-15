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
package org.jkiss.dbeaver.model.task;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Task utils
 */
public class DBTaskUtils {
    private static final Log log = Log.getLog(DBTaskUtils.class);

    public static final String TASK_VARIABLES = "taskVariables";
    public static final String TASK_PROMPT_VARIABLES = "promptTaskVariables";
    public static final String TASK_CONTEXT = "taskContext";

    @NotNull
    public static Map<String, Object> getVariables(@NotNull DBTTask task) {
        Map<String, Object> state = task.getProperties();

        Map<String, Object> variables = (Map<String, Object>) state.get(TASK_VARIABLES);
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        return variables;
    }

    public static void setVariables(@NotNull DBTTask task, @Nullable Map<String, Object> variables) {
        setVariables(task.getProperties(), variables);
    }

    public static void setVariables(@NotNull Map<String, Object> taskState, @Nullable Map<String, Object> variables) {
        if (!CommonUtils.isEmpty(variables)) {
            taskState.put(DBTaskUtils.TASK_VARIABLES, variables);
        } else {
            taskState.remove(DBTaskUtils.TASK_VARIABLES);
        }
    }

    public static DBTTaskContext extractContext(@NotNull DBCExecutionContext executionContext) {
        DBTTaskContext context = new DBTTaskContext();

        DBCExecutionContextDefaults defaults = executionContext.getContextDefaults();
        if (defaults != null) {
            DBSCatalog defaultCatalog = defaults.getDefaultCatalog();
            if (defaultCatalog != null) {
                context.setDefaultCatalog(defaultCatalog.getName());
            }
            DBSSchema defaultSchema = defaults.getDefaultSchema();
            if (defaultSchema != null) {
                context.setDefaultSchema(defaultSchema.getName());
            }
        }
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
        if (txnManager != null) {
            try {
                context.setAutoCommit(txnManager.isAutoCommit());
                DBPTransactionIsolation isolation = txnManager.getTransactionIsolation();
                if (isolation != null) {
                    context.setTransactionIsolation(isolation.getCode());
                }
            } catch (Throwable e) {
                log.debug(e);
            }
        }
        return context;
    }

    @Nullable
    public static DBTTaskContext loadTaskContext(@NotNull Map<String, Object> taskState) {
        Map<String, Object> contextMap = (Map<String, Object>) taskState.get(TASK_CONTEXT);
        if (contextMap == null) {
            return null;
        }
        DBTTaskContext context = new DBTTaskContext();
        context.setDefaultCatalog(CommonUtils.toString(contextMap.get("defaultCatalog"), null));
        context.setDefaultSchema(CommonUtils.toString(contextMap.get("defaultSchema"), null));
        context.setAutoCommit(CommonUtils.toBoolean(contextMap.get("autoCommit")));
        context.setTransactionIsolation(CommonUtils.toInt(contextMap.get("transactionIsolation"), -1));
        return context;
    }

    public static void saveTaskContext(@NotNull Map<String, Object> taskState, @Nullable DBTTaskContext context) {
        if (context == null) {
            taskState.remove(TASK_CONTEXT);
            return;
        }
        Map<String, Object> taskContext = new LinkedHashMap<>();

        if (context.getDefaultCatalog() != null) {
            taskContext.put("defaultCatalog", context.getDefaultCatalog());
        }
        if (context.getDefaultSchema() != null) {
            taskContext.put("defaultSchema", context.getDefaultSchema());
        }

        taskContext.put("autoCommit", context.isAutoCommit());
        if (context.getTransactionIsolation() >= 0) {
            taskContext.put("transactionIsolation", context.getTransactionIsolation());
        }
        taskState.put(TASK_CONTEXT, taskContext);
    }

    public static void initFromContext(DBRProgressMonitor monitor, DBTTask task, DBCExecutionContext executionContext) throws DBException {
        DBTTaskContext context = loadTaskContext(task.getProperties());
        if (context != null) {
            DBExecUtils.setExecutionContextDefaults(monitor, executionContext.getDataSource(), executionContext,
                context.getDefaultCatalog(),
                null,
                context.getDefaultSchema());
        }
    }

    public static boolean isTaskExists(DBTTask task) {
        if (task == null) {
            return false;
        }
        return task.getProject().getTaskManager().getTaskById(task.getId()) != null;
    }

    public static void collectTaskVariables(
        @NotNull DBTTask task,
        @NotNull Predicate<DBTTask> predicate,
        @NotNull Map<DBTTask, Map<String, Object>> variables
    ) throws DBException {
        if (predicate.test(task)) {
            final Map<String, Object> vars = getVariables(task);
            if (!vars.isEmpty()) {
                variables.put(task, vars);
            }
        }

        final DBTTaskHandler handler = task.getType().createHandler();
        if (handler instanceof DBTTaskVariableCollector collector) {
            collector.collectTaskVariables(task, predicate, variables);
        }
    }

    public static void confirmTaskOrThrow(DBTTask task, Log taskLog, PrintStream logWriter) throws InterruptedException {
        if (!confirmTask(task, taskLog, logWriter, (sb, t) -> false)) {
            throw new InterruptedException(ModelMessages.tasks_restore_confirmation_cancelled_message);
        }
    }

    public static void confirmTaskOrThrow(DBTTask task, Log taskLog, PrintStream logWriter, TaskConfirmationsCollector extraConfirmationsCollector) throws InterruptedException {
        if (!confirmTask(task, taskLog, logWriter, extraConfirmationsCollector)) {
            throw new InterruptedException(ModelMessages.tasks_restore_confirmation_cancelled_message);
        }
    }

    public static boolean confirmTask(DBTTask task, Log taskLog, PrintStream logWriter) {
        return confirmTask(task, taskLog, logWriter, (sb, t) -> false);
    }

    public static boolean confirmTask(DBTTask task, Log taskLog, PrintStream logWriter, TaskConfirmationsCollector extraConfirmationsCollector) {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode()) {
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        if (!collectConfirmationMessages(messageBuilder, task, extraConfirmationsCollector)) {
            return true;
        } else {
            messageBuilder.append("\n").append(ModelMessages.tasks_restore_confirmation_message);
        }
        if (DBWorkbench.getPlatformUI().confirmAction(NLS.bind(ModelMessages.tasks_restore_confirmation_title, task.getName()), messageBuilder.toString())) {
            return true;
        }
        
        if (taskLog != null) {
            taskLog.warn(ModelMessages.tasks_restore_confirmation_cancelled_message);
        }
        if (logWriter != null) {
            logWriter.println();
            logWriter.println(ModelMessages.tasks_restore_confirmation_cancelled_message);
            logWriter.println();
        }
        return false;
    }

    public static boolean collectConfirmationMessages(StringBuilder messageBuilder, DBTTask task, TaskConfirmationsCollector extraConfirmationsCollector) {
        boolean confirmationRequired = false;
        String messageOrNull = task.getType().confirmationMessageIfNeeded();
        if (messageOrNull != null) {
            Optional<String> inputFileKey = task.getProperties().keySet().stream().filter(k -> k.contains("inputFile")).findFirst();
            String inputFile = inputFileKey.isPresent() ? task.getProperties().get(inputFileKey.get()).toString() : "file";
            String dbObjectNames = "";
            Object dbObjectIdsObj = task.getProperties().get("databaseObjects");
            if (dbObjectIdsObj != null) {
                List<String> dbObjectIds = (List<String>)dbObjectIdsObj;
                dbObjectNames = dbObjectIds.stream().map(id -> DBUtils.getObjectNameFromId(id)).collect(Collectors.joining(", "));
            }
            messageBuilder.append(NLS.bind(messageOrNull, dbObjectNames, inputFile)).append("\n");
            confirmationRequired |= true;
        }
        confirmationRequired |= extraConfirmationsCollector.collect(messageBuilder, task); 
        return confirmationRequired;
    }
    
    public static interface TaskConfirmationsCollector {
        boolean collect(StringBuilder messageBuilder, DBTTask task);
    }
}
