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
package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseAttribute;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseAttributeIdentity;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseCollation;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseOid;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSequence;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableColumn;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseViewBase;
import org.jkiss.dbeaver.ext.kingbase.model.data.type.KingbaseTypeHandler;
import org.jkiss.dbeaver.ext.kingbase.model.data.type.KingbaseTypeHandlerProvider;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.DBUtils;
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

/**
 * Kingbase table column manager
 */
public class KingbaseTableColumnManager extends SQLTableColumnManager<KingbaseTableColumn, KingbaseTableBase>
        implements DBEObjectRenamer<KingbaseTableColumn>, DBPScriptObjectExt2 {

    String OPTION_NON_STRUCT_CREATE_ACTION = "non.struct.create.action";

    protected final ColumnModifier<KingbaseTableColumn> KingbaseDataTypeModifier = (monitor, column, sql, command) -> {
        sql.append(' ');

        final KingbaseDataType dataType = column.getDataType();
        if (dataType != null) {
            sql.append(dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));

            final KingbaseTypeHandler handler = KingbaseTypeHandlerProvider.getTypeHandler(dataType);
            if (handler != null) {
                sql.append(handler.getTypeModifiersString(dataType, column.getTypeMod()));
            }
        } else {
            sql.append(column.getTypeName());
        }
    };

    protected final ColumnModifier<KingbaseTableColumn> KingbaseDefaultModifier = (monitor, column, sql, command) -> {
        String defaultValue = column.getDefaultValue();
        if (!CommonUtils.isEmpty(defaultValue) && defaultValue.startsWith("nextval")) {
            // Remove serial type default value from DDL
            if (KingbaseConstants.SERIAL_TYPES.containsKey(column.getDataType().getName())) {
                return;
            }
        }
        DefaultModifier.appendModifier(monitor, column, sql, command);
    };

    protected final ColumnModifier<KingbaseTableColumn> KingbaseIdentityModifier = (monitor, column, sql, command) -> {
        KingbaseAttributeIdentity identity = column.getIdentity();
        if (identity != null) {
            sql.append(" ").append(identity.getDefinitionClause());
        }
        if (column.getDepObjectId() != 0) {
            // This column has dependency with object
            try {
                KingbaseTableBase table = column.getSchema().getTable(monitor, column.getDepObjectId());
                if (table instanceof KingbaseSequence) {
                    sql.append("(");
                    ((KingbaseSequence) table).getSequenceBody(monitor, sql, false);
                    sql.append(")");
                }
            } catch (DBException e) {
                log.debug("Can't find the depended object.");
            }
        }
    };

    protected final ColumnModifier<KingbaseTableColumn> KingbaseCollateModifier = (monitor, column, sql, command) -> {
        try {
            KingbaseCollation collation = column.getCollation(monitor);
            if (collation != null && !KingbaseConstants.COLLATION_DEFAULT.equals(collation.getName())) {
                sql.append(" COLLATE \"").append(collation.getName()).append("\"");
            }
        } catch (DBException e) {
            log.debug(e);
        }
    };

    protected final ColumnModifier<KingbaseTableColumn> KingbaseCommentModifier = (monitor, column, sql, command) -> {
        String comment = column.getDescription();
        boolean createNonStructAction = command instanceof DBECommandWithOptions && ((DBECommandWithOptions) command).getOptions().containsKey(OPTION_NON_STRUCT_CREATE_ACTION); // Column already has comment in this action
        if (!createNonStructAction && !CommonUtils.isEmpty(comment)) {
            sql.append(" -- ").append(CommonUtils.getSingleLineString(comment));
        }
    };

    protected final ColumnModifier<KingbaseTableColumn> KingbaseGeneratedModifier = (monitor, column, sql, command) -> {
        String generatedValue = column.getGeneratedValue();
        if (!CommonUtils.isEmpty(generatedValue)) {
            sql.append(" GENERATED ALWAYS AS (").append(generatedValue).append(") STORED");
        }
    };

    @Override
    public boolean canEditObject(KingbaseTableColumn object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, KingbaseTableColumn> getObjectsCache(KingbaseTableColumn object)
    {
        return object.getParentObject().getContainer().getSchema().getTableCache().getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier[] getSupportedModifiers(KingbaseTableColumn column, Map<String, Object> options)
    {
        ColumnModifier[] modifiers = {
            KingbaseDataTypeModifier,
            KingbaseDefaultModifier,
            KingbaseIdentityModifier,
            KingbaseCollateModifier,
            KingbaseGeneratedModifier
        };
        if (column.getDataSource().getServerType().supportsColumnsRequiring()) {
            modifiers = ArrayUtils.add(ColumnModifier.class, modifiers, NullNotNullModifier);
        }
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
            modifiers = ArrayUtils.add(ColumnModifier.class, modifiers, KingbaseCommentModifier);
        }
        return modifiers;
    }

    @Override
    public StringBuilder getNestedDeclaration(@NotNull DBRProgressMonitor monitor, @NotNull KingbaseTableBase owner, @NotNull DBECommandAbstract<KingbaseTableColumn> command, @NotNull Map<String, Object> options)
    {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        final KingbaseAttribute column = command.getObject();
        return decl;
    }

    @Override
    protected KingbaseTableColumn createDatabaseObject(@NotNull final DBRProgressMonitor monitor, @NotNull final DBECommandContext context, final Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        KingbaseTableBase table = (KingbaseTableBase) container;

        final KingbaseTableColumn column;
        if (copyFrom instanceof KingbaseTableColumn) {
            column = new KingbaseTableColumn(monitor, table, (KingbaseTableColumn)copyFrom);
        } else {
            column = new KingbaseTableColumn(table);
            column.setName(getNewColumnName(monitor, context, table));
            final KingbaseDataType dataType = table.getDatabase().getDataType(monitor, KingbaseOid.VARCHAR);
            column.setDataType(dataType); //$NON-NLS-1$
            column.setOrdinalPosition(-1);
        }
        return column;
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        options.put(OPTION_NON_STRUCT_CREATE_ACTION, true);
        KingbaseTableBase table = command.getObject().getParentObject();
        String sql = "ALTER " + table.getTableTypeName() + " " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) + " ADD " +
            getNestedDeclaration(monitor, table, command, options);
        actions.add(new SQLDatabasePersistAction("Create new table column", sql));
        if (!CommonUtils.isEmpty(command.getObject().getDescription())) {
            addColumnCommentAction(actions, command.getObject());
        }
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options)
    {
        final KingbaseAttribute column = command.getObject();
        boolean isAtomic = column.getDataSource().getServerType().isAlterTableAtomic();
        KingbaseTableBase table = (KingbaseTableBase) column.getTable();
        String prefix = "ALTER " + table.getTableTypeName() + " " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) +
            " ALTER COLUMN " + DBUtils.getQuotedIdentifier(column) + " ";
        final String fullTypeName = column.getFullTypeName();
        String typeClause = fullTypeName;
        if (column.getDataSource().getServerType().supportsAlterTableColumnWithUSING()) {
            typeClause += " USING ";
            typeClause += column.getDataSource().getSQLDialect().getTypeCastClause(column, DBUtils.getQuotedIdentifier(column), true);
            typeClause += "::" + fullTypeName;
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

    public static void addColumnCommentAction(List<DBEPersistAction> actionList, KingbaseAttribute column) {
        actionList.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
            DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
            " IS " + SQLUtils.quoteString(column, CommonUtils.notEmpty(column.getDescription()))));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseTableColumn object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
        final KingbaseTableBase table = object.getTable();
        if (table.isPersisted() && table instanceof KingbaseViewBase) {
            table.setObjectDefinitionText(null);
            commandContext.addCommand(new EmptyCommand(table), new RefreshObjectReflector(), true);
        }
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options)
    {
        final KingbaseAttribute column = command.getObject();
        KingbaseTableBase table = (KingbaseTableBase) column.getTable();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER " + table.getTableTypeName() + " " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) +
                    " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return DBPScriptObject.OPTION_INCLUDE_COMMENTS.equals(option);
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        KingbaseTableColumn column = command.getObject();
        KingbaseTableBase table = column.getParentObject();
        String ddl = "ALTER " + table.getTableTypeName() + " " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) +
            " DROP COLUMN " + DBUtils.getQuotedIdentifier(column);
        actions.add(new SQLDatabasePersistAction("Drop table column", ddl));
    }
}
