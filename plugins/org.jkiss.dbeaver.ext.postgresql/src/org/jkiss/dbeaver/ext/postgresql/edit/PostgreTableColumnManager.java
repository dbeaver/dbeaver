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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.data.type.PostgreTypeHandler;
import org.jkiss.dbeaver.ext.postgresql.model.data.type.PostgreTypeHandlerProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandWithOptions;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Postgre table column manager
 */
public class PostgreTableColumnManager extends SQLTableColumnManager<PostgreTableColumn, PostgreTableBase>
        implements DBEObjectRenamer<PostgreTableColumn>, DBPScriptObjectExt2 {

    String OPTION_NON_STRUCT_CREATE_ACTION = "non.struct.create.action";

    protected final ColumnModifier<PostgreTableColumn> PostgreDataTypeModifier = (monitor, column, sql, command) -> {
        sql.append(' ');

        final PostgreDataType dataType = column.getDataType();
        if (dataType != null) {
            sql.append(dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));

            final PostgreTypeHandler handler = PostgreTypeHandlerProvider.getTypeHandler(dataType);
            if (handler != null) {
                sql.append(handler.getTypeModifiersString(dataType, column.getTypeMod()));
            }
        } else {
            sql.append(column.getTypeName());
        }

        if (column.getTable() instanceof PostgreTableForeign) {
            String[] foreignTableColumnOptions = column.getForeignTableColumnOptions();
            if (foreignTableColumnOptions != null && foreignTableColumnOptions.length != 0) {
                sql.append(" OPTIONS").append(PostgreUtils.getOptionsString(foreignTableColumnOptions));
            }
        }
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreDefaultModifier = (monitor, column, sql, command) -> {
        String defaultValue = column.getDefaultValue();
        if (!CommonUtils.isEmpty(defaultValue) && defaultValue.startsWith("nextval")) {
            // Remove serial type default value from DDL
            if (PostgreConstants.SERIAL_TYPES.containsKey(column.getDataType().getName())) {
                return;
            }
        }
        DefaultModifier.appendModifier(monitor, column, sql, command);
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreIdentityModifier = (monitor, column, sql, command) -> {
        PostgreAttributeIdentity identity = column.getIdentity();
        if (identity != null) {
            sql.append(" ").append(identity.getDefinitionClause());
        }
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreCollateModifier = (monitor, column, sql, command) -> {
        try {
            PostgreCollation collation = column.getCollation(monitor);
            if (collation != null && !PostgreConstants.COLLATION_DEFAULT.equals(collation.getName())) {
                sql.append(" COLLATE \"").append(collation.getName()).append("\"");
            }
        } catch (DBException e) {
            log.debug(e);
        }
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreCommentModifier = (monitor, column, sql, command) -> {
        String comment = column.getDescription();
        boolean createNonStructAction = command instanceof DBECommandWithOptions && ((DBECommandWithOptions) command).getOptions().containsKey(OPTION_NON_STRUCT_CREATE_ACTION); // Column already has comment in this action
        if (!createNonStructAction && !CommonUtils.isEmpty(comment)) {
            sql.append(" -- ").append(CommonUtils.getSingleLineString(comment));
        }
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreGeneratedModifier = (monitor, column, sql, command) -> {
        String generatedValue = column.getGeneratedValue();
        if (!CommonUtils.isEmpty(generatedValue)) {
            sql.append(" GENERATED ALWAYS AS (").append(generatedValue).append(") STORED");
        }
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTableColumn> getObjectsCache(PostgreTableColumn object)
    {
        return object.getParentObject().getContainer().getSchema().getTableCache().getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier[] getSupportedModifiers(PostgreTableColumn column, Map<String, Object> options)
    {
        ColumnModifier[] modifiers = {PostgreDataTypeModifier, NullNotNullModifier, PostgreDefaultModifier, PostgreIdentityModifier, PostgreCollateModifier, PostgreGeneratedModifier};
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
            modifiers = ArrayUtils.add(ColumnModifier.class, modifiers, PostgreCommentModifier);
        }
        return modifiers;
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, PostgreTableBase owner, DBECommandAbstract<PostgreTableColumn> command, Map<String, Object> options)
    {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        final PostgreAttribute column = command.getObject();
        return decl;
    }

    @Override
    protected PostgreTableColumn createDatabaseObject(final DBRProgressMonitor monitor, final DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        PostgreTableBase table = (PostgreTableBase) container;

        final PostgreTableColumn column;
        if (copyFrom instanceof PostgreTableColumn) {
            column = new PostgreTableColumn(monitor, table, (PostgreTableColumn)copyFrom);
        } else {
            column = new PostgreTableColumn(table);
            column.setName(getNewColumnName(monitor, context, table));
            final PostgreDataType dataType = table.getDatabase().getDataType(monitor, PostgreOid.VARCHAR);
            column.setDataType(dataType); //$NON-NLS-1$
            column.setOrdinalPosition(-1);
        }
        return column;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        options.put(OPTION_NON_STRUCT_CREATE_ACTION, true);
        super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        if (!CommonUtils.isEmpty(command.getObject().getDescription())) {
            addColumnCommentAction(actions, command.getObject());
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final PostgreAttribute column = command.getObject();
        boolean isAtomic = column.getDataSource().getServerType().isAlterTableAtomic();
        // PostgreSQL can't perform all changes by one query
//        ALTER [ COLUMN ] column [ SET DATA ] TYPE data_type [ COLLATE collation ] [ USING expression ]
//        ALTER [ COLUMN ] column SET DEFAULT expression
//        ALTER [ COLUMN ] column DROP DEFAULT
//        ALTER [ COLUMN ] column { SET | DROP } NOT NULL
//        ALTER [ COLUMN ] column SET STATISTICS integer
//        ALTER [ COLUMN ] column SET ( attribute_option = value [, ... ] )
//        ALTER [ COLUMN ] column RESET ( attribute_option [, ... ] )
//        ALTER [ COLUMN ] column SET STORAGE { PLAIN | EXTERNAL | EXTENDED | MAIN }
        String prefix = "ALTER TABLE " + DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + " ALTER COLUMN " + DBUtils.getQuotedIdentifier(column) + " ";
        String typeClause = column.getFullTypeName();
        if (column.getDataSource().isServerVersionAtLeast(8, 0) && column.getDataType() != null) {
            typeClause += " USING " + DBUtils.getQuotedIdentifier(column) + "::" + column.getDataType().getName();
        }
        if (command.hasProperty("fullTypeName") || command.hasProperty("maxLength") || command.hasProperty("precision") || command.hasProperty("scale")) {
            actionList.add(new SQLDatabasePersistActionAtomic("Set column type", prefix + "TYPE " + typeClause, isAtomic));
        }
        if (command.hasProperty(DBConstants.PROP_ID_REQUIRED)) {
            actionList.add(new SQLDatabasePersistActionAtomic("Set column nullability", prefix + (column.isRequired() ? "SET" : "DROP") + " NOT NULL", isAtomic));
        }

        if (command.hasProperty(DBConstants.PROP_ID_DEFAULT_VALUE)) {
            if (CommonUtils.isEmpty(column.getDefaultValue())) {
                actionList.add(new SQLDatabasePersistActionAtomic("Drop column default", prefix + "DROP DEFAULT", isAtomic));
            } else {
                actionList.add(new SQLDatabasePersistActionAtomic("Set column default", prefix + "SET DEFAULT " + column.getDefaultValue(), isAtomic));
            }
        }
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            addColumnCommentAction(actionList, column);
        }
    }

    public static void addColumnCommentAction(List<DBEPersistAction> actionList, PostgreAttribute column) {
        actionList.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
            DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
            " IS " + SQLUtils.quoteString(column, CommonUtils.notEmpty(column.getDescription()))));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreTableColumn object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
        final PostgreTableBase table = object.getTable();
        if (table.isPersisted() && table instanceof PostgreViewBase) {
            table.setObjectDefinitionText(null);
            commandContext.addCommand(new EmptyCommand(table), new RefreshObjectReflector(), true);
        }
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final PostgreAttribute column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return DBPScriptObject.OPTION_INCLUDE_COMMENTS.equals(option);
    }
}
