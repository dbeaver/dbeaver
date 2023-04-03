/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.mssql.edit.generic;

import org.jkiss.dbeaver.ext.generic.edit.GenericTriggerManager;
import org.jkiss.dbeaver.ext.mssql.model.generic.SQLServerGenericTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * SQLServerTableTriggerManager
 */
public class SQLServerGenericTriggerManager extends GenericTriggerManager {

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        SQLServerGenericTrigger trigger = (SQLServerGenericTrigger) command.getObject();
        DBSObject defaultDatabase = DBUtils.getDefaultContext(trigger.getDataSource(), true).getContextDefaults().getDefaultCatalog();
        if (defaultDatabase != trigger.getTable().getCatalog()) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + DBUtils.getQuotedIdentifier(trigger.getTable().getCatalog()), false)); //$NON-NLS-2$
        }

        super.addObjectDeleteActions(monitor, executionContext, actions, command, options);

        if (defaultDatabase != trigger.getTable().getCatalog()) {
            actions.add(new SQLDatabasePersistAction("Set current schema ", "USE " + DBUtils.getQuotedIdentifier(defaultDatabase), false)); //$NON-NLS-2$
        }
    }
}

