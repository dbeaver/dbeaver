/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericForeignKeyManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.sqlite.SQLiteUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SQLiteTableForeignKeyManager extends GenericForeignKeyManager {
    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canEditObject(GenericTableForeignKey object) {
        return true;
    }

    @Override
    public boolean canDeleteObject(GenericTableForeignKey object) {
        return true;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        SQLiteUtils.createTableAlterActions(
            monitor,
            "Create foreign key " + DBUtils.getQuotedIdentifier(command.getObject()),
            command.getObject().getTable(),
            Collections.emptyList(),
            actions
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        SQLiteUtils.createTableAlterActions(
            monitor,
            "Alter foreign key " + DBUtils.getQuotedIdentifier(command.getObject()),
            command.getObject().getTable(),
            Collections.emptyList(),
            actions
        );
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        SQLiteUtils.createTableAlterActions(
            monitor,
            "Drop foreign key " + DBUtils.getQuotedIdentifier(command.getObject()),
            command.getObject().getTable(),
            Collections.emptyList(),
            actions
        );
    }
}
