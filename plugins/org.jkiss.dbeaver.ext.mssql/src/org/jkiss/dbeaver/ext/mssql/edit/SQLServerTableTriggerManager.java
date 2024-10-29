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

package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerExecutionContext;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableBase;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * SQLServerTableTriggerManager
 */
public class SQLServerTableTriggerManager extends SQLTriggerManager<SQLServerTableTrigger, SQLServerTableBase> {
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableTrigger> getObjectsCache(SQLServerTableTrigger object)
    {
        return object.getSchema().getTriggerCache();
    }

    @Override
    protected SQLServerTableTrigger createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        SQLServerTableBase table = (SQLServerTableBase) container;
        String newTriggerName = "NewTrigger";
        SQLServerTableTrigger newTrigger = new SQLServerTableTrigger(table, newTriggerName);
        newTrigger.setBody(
            "CREATE OR ALTER TRIGGER " + newTriggerName + " ON " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "\n" +
            "AFTER INSERT\n" +
            "AS\n" +
            ";\n"
        );
        return newTrigger;
    }

    protected void createOrReplaceTriggerQuery(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull SQLServerTableTrigger trigger, boolean create) {
        DBSObject defaultDatabase = ((SQLServerExecutionContext)executionContext).getDefaultCatalog();
        SQLServerTableBase table = trigger.getTable();
        if (defaultDatabase != table.getDatabase()) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + //$NON-NLS-2$
                DBUtils.getQuotedIdentifier(table.getDatabase()), false));
        }

        if (create) {
            actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getBody(), true)); //$NON-NLS-2$
        } else {
            actions.add(new SQLDatabasePersistAction("Alter trigger",
                SQLServerUtils.changeCreateToAlterDDL(trigger.getDataSource().getSQLDialect(), trigger.getBody()), true)); //$NON-NLS-2$
        }

        if (defaultDatabase != table.getDatabase()) {
            String defaultCatalogName = getDefaultCatalogName((SQLServerExecutionContext) executionContext, defaultDatabase, table);
            actions.add(new SQLDatabasePersistAction("Set current database ", //$NON-NLS-1$
                "USE " + defaultCatalogName, false)); //$NON-NLS-1$
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        SQLServerTableTrigger trigger = command.getObject();
        DBSObject defaultDatabase = ((SQLServerExecutionContext)executionContext).getDefaultCatalog();
        SQLServerTableBase table = trigger.getTable();
        if (defaultDatabase != table.getDatabase()) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + //$NON-NLS-2$
                DBUtils.getQuotedIdentifier(table.getDatabase()), false));
        }

        super.addObjectDeleteActions(monitor, executionContext, actions, command, options);

        if (defaultDatabase != table.getDatabase()) {
            String defaultCatalogName = getDefaultCatalogName((SQLServerExecutionContext) executionContext, defaultDatabase, table);
            actions.add(new SQLDatabasePersistAction("Set current database ",
                "USE " + defaultCatalogName, false)); //$NON-NLS-1$
        }
    }

    @NotNull
    private static String getDefaultCatalogName(
        @NotNull SQLServerExecutionContext executionContext,
        @Nullable DBSObject defaultDatabase,
        @NotNull SQLServerTableBase table
    ) {
        String defaultCatalogName;
        if (defaultDatabase != null) {
            defaultCatalogName = DBUtils.getQuotedIdentifier(defaultDatabase);
        } else {
            defaultCatalogName = DBUtils.getQuotedIdentifier(
                table.getDataSource(),
                executionContext.getActiveDatabaseName());
        }
        return defaultCatalogName;
    }
}
