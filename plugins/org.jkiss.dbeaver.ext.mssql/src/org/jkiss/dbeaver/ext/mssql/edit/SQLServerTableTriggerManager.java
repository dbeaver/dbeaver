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

package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * SQLServerTableTriggerManager
 */
public class SQLServerTableTriggerManager extends SQLTriggerManager<SQLServerTableTrigger, SQLServerTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableTrigger> getObjectsCache(SQLServerTableTrigger object)
    {
        return object.getSchema().getTriggerCache();
    }

    @Override
    protected SQLServerTableTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final SQLServerTable parent, Object copyFrom)
    {
        String newTriggerName = "NewTrigger";
        SQLServerTableTrigger newTrigger = new SQLServerTableTrigger(parent, newTriggerName);
        newTrigger.setBody(
            "CREATE OR ALTER TRIGGER " + newTriggerName + " ON " + parent.getFullyQualifiedName(DBPEvaluationContext.DDL) + "\n" +
            "AFTER INSERT\n" +
            "AS\n" +
            "END;\n"
        );
        return newTrigger;
    }

    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, SQLServerTableTrigger trigger) {
        DBSObject defaultDatabase = trigger.getDataSource().getDefaultObject();
        if (defaultDatabase != trigger.getTable().getDatabase()) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + DBUtils.getQuotedIdentifier(trigger.getTable().getDatabase()), false)); //$NON-NLS-2$
        }

        actions.add(new SQLDatabasePersistAction("Create trigger",
            trigger.getBody(), true)); //$NON-NLS-2$

        if (defaultDatabase != trigger.getTable().getDatabase()) {
            actions.add(new SQLDatabasePersistAction("Set current database ", "USE " + DBUtils.getQuotedIdentifier(defaultDatabase), false)); //$NON-NLS-2$
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        SQLServerTableTrigger trigger = command.getObject();
        DBSObject defaultDatabase = trigger.getDataSource().getDefaultObject();
        if (defaultDatabase != trigger.getTable().getDatabase()) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + DBUtils.getQuotedIdentifier(trigger.getTable().getDatabase()), false)); //$NON-NLS-2$
        }

        super.addObjectDeleteActions(actions, command, options);

        if (defaultDatabase != trigger.getTable().getDatabase()) {
            actions.add(new SQLDatabasePersistAction("Set current database ", "USE " + DBUtils.getQuotedIdentifier(defaultDatabase), false)); //$NON-NLS-2$
        }
    }
}

