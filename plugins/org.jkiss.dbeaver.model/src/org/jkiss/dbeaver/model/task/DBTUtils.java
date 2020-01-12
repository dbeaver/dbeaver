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
package org.jkiss.dbeaver.model.task;

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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Task utils
 */
public class DBTUtils {
    private static final Log log = Log.getLog(DBTUtils.class);

    public static final String TASK_VARIABLES = "taskVariables";
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

    public static void setVariables(@NotNull Map<String, Object> taskState, @Nullable Map<String, Object> variables) {
        if (!CommonUtils.isEmpty(variables)) {
            taskState.put(DBTUtils.TASK_VARIABLES, variables);
        } else {
            taskState.remove(DBTUtils.TASK_VARIABLES);
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

}
