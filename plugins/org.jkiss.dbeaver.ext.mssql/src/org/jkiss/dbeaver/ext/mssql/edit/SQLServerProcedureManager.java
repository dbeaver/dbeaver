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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * SQLServerProcedureManager
 */
public class SQLServerProcedureManager extends SQLObjectEditor<SQLServerProcedure, SQLServerSchema> {

    @Nullable
    @Override
    public DBSObjectCache<SQLServerSchema, SQLServerProcedure> getObjectsCache(SQLServerProcedure object) {
        return object.getContainer().getProcedureCache();
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (!command.getObject().isPersisted() && CommonUtils.isEmpty(command.getObject().getBody())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    @Override
    protected SQLServerProcedure createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) {
        return new SQLServerProcedure((SQLServerSchema) container);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceProcedureQuery(monitor, executionContext, actions, command.getObject(), true);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceProcedureQuery(monitor, executionContext, actionList, command.getObject(), false);
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        SQLServerDatabase procDatabase = command.getObject().getContainer().getDatabase();
        SQLServerDatabase defaultDatabase = ((SQLServerExecutionContext)executionContext).getDefaultCatalog();
        if (defaultDatabase != procDatabase) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + DBUtils.getQuotedIdentifier(procDatabase), false)); //$NON-NLS-2$
        }

        actions.add(
            new SQLDatabasePersistAction("Drop procedure", "DROP " + command.getObject().getProcedureType() + " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );

        if (defaultDatabase != procDatabase) {
            actions.add(new SQLDatabasePersistAction("Set current database ", "USE " + DBUtils.getQuotedIdentifier(defaultDatabase), false)); //$NON-NLS-2$
        }
    }

    private void createOrReplaceProcedureQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLServerProcedure procedure, boolean create) throws DBException {
        SQLServerDatabase procDatabase = procedure.getContainer().getDatabase();
        SQLServerDatabase defaultDatabase = ((SQLServerExecutionContext)executionContext).getDefaultCatalog();
        if (defaultDatabase != procDatabase) {
            actions.add(new SQLDatabasePersistAction("Set current database", "USE " + DBUtils.getQuotedIdentifier(procDatabase), false)); //$NON-NLS-2$
        }

        if (create) {
            actions.add(new SQLDatabasePersistAction("Create procedure", procedure.getBody()));
        } else {
            actions.add(new SQLDatabasePersistAction("Alter procedure", SQLServerUtils.changeCreateToAlterDDL(procedure.getDataSource().getSQLDialect(), procedure.getBody())));
        }

        if (defaultDatabase != procDatabase) {
            actions.add(new SQLDatabasePersistAction("Set current database ", "USE " + DBUtils.getQuotedIdentifier(defaultDatabase), false)); //$NON-NLS-2$
        }
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, NestedObjectCommand<SQLServerProcedure, PropertyHandler> command, Map<String, Object> options) throws DBException {
        final SQLServerProcedure procedure = command.getObject();
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            SQLServerDatabase database = procedure.getContainer().getDatabase();
            boolean isUpdate = SQLServerUtils.isCommentSet(
                monitor,
                database,
                SQLServerObjectClass.OBJECT_OR_COLUMN,
                procedure.getObjectId(),
                0);
            actions.add(
                new SQLDatabasePersistAction(
                    "Add procedure comment",
                    "EXEC " + SQLServerUtils.getSystemTableName(database, isUpdate ? "sp_updateextendedproperty" : "sp_addextendedproperty") +
                        " 'MS_Description', " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription()) + "," +
                        " 'schema', '" + procedure.getContainer().getName() + "'," +
                        " 'procedure', '" + procedure.getName() + "'"));
        }
    }

}

