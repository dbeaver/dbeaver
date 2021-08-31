/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SQLServer table column manager
 */
public class SQLServerTableColumnManager extends SQLTableColumnManager<SQLServerTableColumn, SQLServerTableBase> implements DBEStructEditor<SQLServerTableColumn>, DBEObjectRenamer<SQLServerTableColumn> {

    protected final ColumnModifier<SQLServerTableColumn> IdentityModifier = (monitor, column, sql, command) -> {
        if (column.isIdentity()) {
            try {
                SQLServerTableColumn.IdentityInfo identityInfo = column.getIdentityInfo(monitor);
                long incrementValue = identityInfo.getIncrementValue();
                if (incrementValue <= 0) incrementValue = 1;
                sql.append(" IDENTITY(").append(identityInfo.getSeedValue()).append(",").append(incrementValue).append(")");
            } catch (DBCException e) {
                log.error("Error reading identity information", e); //$NON-NLS-1$
            }
        }
    };

    protected final ColumnModifier<SQLServerTableColumn> CollateModifier = (monitor, column, sql, command) -> {
        String collationName = column.getCollationName();
        if (!CommonUtils.isEmpty(collationName)) {
            sql.append(" COLLATE ").append(collationName); //$NON-NLS-1$
        }
    };

    protected final ColumnModifier<SQLServerTableColumn> SQLServerDefaultModifier = (monitor, column, sql, command) -> {
        boolean ddlSource = false;
        if (command instanceof DBECommandWithOptions) {
            DBECommandWithOptions commandWithOptions = (DBECommandWithOptions)command;
            if (commandWithOptions.getOptions().containsKey(DBPScriptObject.OPTION_DDL_SOURCE)){
                ddlSource = true;
            }
        }
        if (!column.isPersisted() || ddlSource) {
            DefaultModifier.appendModifier(monitor, column, sql, command);
        }
    };

    protected final ColumnModifier<SQLServerTableColumn> ComputedModifier = (monitor, column, sql, command) -> {
        final String definition = column.getComputedDefinition();
        if (CommonUtils.isNotEmpty(definition)) {
            sql.append(" AS ").append(definition);
        }
        if (column.isComputedPersisted()) {
            sql.append(" PERSISTED");
        }
    };

    private static final Class<?>[] CHILD_TYPES = {
        SQLServerExtendedProperty.class,
    };

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public Collection<? extends DBSObject> getChildObjects(DBRProgressMonitor monitor, SQLServerTableColumn object, Class<? extends DBSObject> childType) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableColumn> getObjectsCache(SQLServerTableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier[] getSupportedModifiers(SQLServerTableColumn column, Map<String, Object> options)
    {
        if (CommonUtils.isNotEmpty(column.getComputedDefinition())) {
            return new ColumnModifier[]{ComputedModifier, NotNullModifier};
        }
        return new ColumnModifier[] {DataTypeModifier, IdentityModifier, CollateModifier, SQLServerDefaultModifier, NullNotNullModifier};
    }

    @Override
    public boolean canEditObject(SQLServerTableColumn object) {
        return !isTableType(object) && super.canEditObject(object);
    }

    @Override
    public boolean canDeleteObject(SQLServerTableColumn object) {
        return !isTableType(object) && super.canDeleteObject(object);
    }

    private boolean isTableType(SQLServerTableColumn column) {
        return column.getTable() instanceof SQLServerTableType;
    }

    @Override
    protected SQLServerTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException
    {
        SQLServerTable table = (SQLServerTable) container;

        DBSDataType columnType = findBestDataType(table, "varchar"); //$NON-NLS-1$

        final SQLServerTableColumn column = new SQLServerTableColumn(table);
        column.setName(getNewColumnName(monitor, context, table));
        column.setDataType((SQLServerDataType) columnType);
        column.setTypeName(columnType == null ? "varchar" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.VARCHAR : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final SQLServerTableColumn column = command.getObject();
        int totalProps = command.getProperties().size();
        boolean hasComment = command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null;
        if (hasComment) totalProps--;
        if (column.isPersisted() && command.hasProperty("defaultValue")) {
            totalProps--;

            // [Re]create default constraint. Classic MS-style pain in the ass
            addDropConstraintAction(actionList, column);

            String defaultValue = column.getDefaultValue();
            if (!CommonUtils.isEmpty(defaultValue)) {
                StringBuilder sql = new StringBuilder();
                sql.append("ALTER TABLE ").append(column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ADD "); //$NON-NLS-1$ //$NON-NLS-2$
                DefaultModifier.appendModifier(monitor, column, sql, command);
                sql.append(" FOR ").append(DBUtils.getQuotedIdentifier(column)); //$NON-NLS-1$
                actionList.add(new SQLDatabasePersistAction("Alter default value", sql.toString())); //$NON-NLS-1$
            }
        }
        if (hasComment) {
            boolean isUpdate = SQLServerUtils.isCommentSet(
                monitor,
                column.getTable().getDatabase(),
                SQLServerObjectClass.OBJECT_OR_COLUMN,
                column.getTable().getObjectId(),
                column.getObjectId());
            actionList.add(
                new SQLDatabasePersistAction(
                    "Add column comment",
                    "EXEC " + SQLServerUtils.getSystemTableName(column.getTable().getDatabase(), isUpdate ? "sp_updateextendedproperty" : "sp_addextendedproperty") +
                        " 'MS_Description', " + SQLUtils.quoteString(column, column.getDescription()) + "," +
                        " 'schema', " + SQLUtils.quoteString(column, column.getTable().getSchema().getName()) + "," +
                        " 'table', " + SQLUtils.quoteString(column, column.getTable().getName()) + "," +
                        " 'column', " + SQLUtils.quoteString(column, column.getName())));
        }
        if (totalProps > 0) {
            actionList.add(new SQLDatabasePersistAction(
                "Modify column",
                "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                    " ALTER COLUMN " + getNestedDeclaration(monitor, column.getTable(), command, options))); //$NON-NLS-1$
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<SQLServerTableColumn, SQLServerTableBase>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        addDropConstraintAction(actions, command.getObject());
        super.addObjectDeleteActions(monitor, executionContext, actions, command, options);
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerTableColumn object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final SQLServerTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                    "EXEC " + SQLServerUtils.getSystemTableName(column.getTable().getDatabase(), "sp_rename") +
                    " N'" + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DML) + "." + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                    "' , N'" + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName()) + "', 'COLUMN'")
        );
    }

    private static void addDropConstraintAction(@NotNull List<DBEPersistAction> actions, @NotNull SQLServerTableColumn column) {
        if (column.getDefaultConstraintName() != null) {
            actions.add(new SQLDatabasePersistAction(
                "Drop default constraint",
                "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " DROP CONSTRAINT " + DBUtils.getQuotedIdentifier(column.getDataSource(), column.getDefaultConstraintName())
            ));
        }
    }
}
