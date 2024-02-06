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
package org.jkiss.dbeaver.ext.mssql.edit.generic;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.generic.SQLServerGenericProcedure;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * SQL Server Generic procedure manager
 */
public class SQLServerGenericProcedureManager extends GenericProcedureManager {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canEditObject(GenericProcedure object) {
        return true;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException {
        GenericProcedure procedure = command.getObject();
        if (CommonUtils.isEmpty(procedure.getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (!procedure.isPersisted() && procedure instanceof SQLServerGenericProcedure proc && CommonUtils.isEmpty(proc.getSource())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    @Override
    protected String getBaseObjectName() {
        return "new_procedure";
    }

    @Override
    protected GenericProcedure createDatabaseObject(
        DBRProgressMonitor monitor,
        DBECommandContext context,
        Object container,
        Object from,
        Map<String, Object> options
    ) throws DBException {
        return new SQLServerGenericProcedure((GenericStructContainer) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        ObjectCreateCommand command,
        Map<String, Object> options
    ) throws DBCException {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        // Always DROP PROCEDURE (SQL Server doesn't support functions?)
        // Do not use database name (not supported)
        GenericProcedure object = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP PROCEDURE " + DBUtils.getQuotedIdentifier(object.getContainer()) + "." + DBUtils.getQuotedIdentifier(object))
        );
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull SQLObjectEditor<GenericProcedure, GenericStructContainer>.ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        actionList.add(new SQLDatabasePersistAction(
            "Create procedure",
            SQLServerUtils.changeCreateToCreateOrReplace(command.getObject().getSource())));
    }

    private void createOrReplaceProcedureQuery(@NotNull List<DBEPersistAction> actions, @NotNull GenericProcedure procedure) {
        actions.add(
            new SQLDatabasePersistAction("Create procedure", procedure.getSource()));
    }

}
