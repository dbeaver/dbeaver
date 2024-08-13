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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObjectClass;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerProcedure;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerSchema;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * SQLServerProcedureManager
 */
public class SQLServerProcedureManager extends SQLServerObjectManager<SQLServerProcedure, SQLServerSchema>
    implements DBEObjectRenamer<SQLServerProcedure> {

    @Nullable
    @Override
    public DBSObjectCache<SQLServerSchema, SQLServerProcedure> getObjectsCache(SQLServerProcedure object) {
        return object.getContainer().getProcedureCache();
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
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
    protected SQLServerProcedure createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object copyFrom, @NotNull Map<String, Object> options) {
        return new SQLServerProcedure((SQLServerSchema) container);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) throws DBException {
        createOrReplaceProcedureQuery(monitor, executionContext, actions, command.getObject(), true);
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceProcedureQuery(monitor, executionContext, actionList, command.getObject(), false);
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        addDatabaseSwitchAction1(executionContext, actions, command.getObject().getContainer().getDatabase());

        actions.add(
            new SQLDatabasePersistAction("Drop procedure", "DROP " + command.getObject().getProcedureType() + " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );

        addDatabaseSwitchAction2(executionContext, actions, command.getObject().getContainer().getDatabase());
    }

    private void createOrReplaceProcedureQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLServerProcedure procedure, boolean create) throws DBException {
        addDatabaseSwitchAction1(executionContext, actions, procedure.getContainer().getDatabase());

        if (create) {
            actions.add(new SQLDatabasePersistAction("Create procedure", procedure.getBody()));
        } else {
            actions.add(new SQLDatabasePersistAction("Alter procedure", SQLServerUtils.changeCreateToAlterDDL(procedure.getDataSource().getSQLDialect(), procedure.getBody())));
        }

        addDatabaseSwitchAction2(executionContext, actions, procedure.getContainer().getDatabase());
    }

    @Override
    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        final SQLServerProcedure procedure = command.getObject();
        if (procedure.getDatabase() == null) {
            return;
        }
        actions.add(
            new SQLDatabasePersistAction(
                "Rename procedure",
                "EXEC " + SQLServerUtils.getSystemTableName(procedure.getDatabase(), "sp_rename") +
                    " N'" + procedure.getContainer().getFullyQualifiedName(DBPEvaluationContext.DML) + "." + DBUtils.getQuotedIdentifier(procedure.getDataSource(), command.getOldName()) +
                    "', " + SQLUtils.quoteString(procedure.getDataSource(), command.getNewName())));
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<SQLServerProcedure, PropertyHandler> command, @NotNull Map<String, Object> options) throws DBException {
        final SQLServerProcedure procedure = command.getObject();
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            SQLServerDatabase database = procedure.getContainer().getDatabase();
            if (procedure.getDatabase() == null) {
                return;
            }
            String procedureType;
            if (procedure.getProcedureType().equals(DBSProcedureType.FUNCTION)) {
                procedureType = "function";
            } else {
                procedureType = "procedure";
            }
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
                        " 'MS_Description', " + SQLUtils.quoteString(procedure, procedure.getDescription()) + "," +
                        " 'schema', " + SQLUtils.quoteString(procedure, procedure.getContainer().getName()) + "," +
                        "'" + procedureType + "' ," + SQLUtils.quoteString(procedure, procedure.getName())));
        }
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull SQLServerProcedure object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }
}

