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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Postgre table column manager
 */
public class PostgreTableColumnManager extends SQLTableColumnManager<PostgreTableColumn, PostgreTableBase>
        implements DBEObjectRenamer<PostgreTableColumn>, DBPScriptObjectExt2 {

    protected final ColumnModifier<PostgreTableColumn> PostgreDataTypeModifier = (monitor, column, sql, command) -> {
        sql.append(' ');
        final PostgreDataType dataType = column.getDataType();
        final PostgreDataType rawType = null;//dataType.getElementType(monitor);
        if (rawType != null) {
            sql.append(rawType.getFullyQualifiedName(DBPEvaluationContext.DDL));
        } else {
            sql.append(dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
        }
        getColumnDataTypeModifiers(monitor, column, sql);
    };

    public static StringBuilder getColumnDataTypeModifiers(DBRProgressMonitor monitor, DBSTypedObject column, StringBuilder sql) {
        if (column instanceof PostgreTableColumn) {
            PostgreTableColumn postgreColumn = (PostgreTableColumn) column;
            final PostgreDataType dataType = postgreColumn.getDataType();
            final PostgreDataType rawType = null;//dataType.getElementType(monitor);
            switch (dataType.getDataKind()) {
                case STRING:
                    final long length = postgreColumn.getMaxLength();
                    if (length > 0 && length < Integer.MAX_VALUE) {
                        sql.append('(').append(length).append(')');
                    }
                    break;
                case NUMERIC:
                    if (dataType.getTypeID() == Types.NUMERIC) {
                        final int precision = CommonUtils.toInt(postgreColumn.getPrecision());
                        final int scale = CommonUtils.toInt(postgreColumn.getScale());
                        if (scale > 0 || precision > 0) {
                            sql.append('(');
                            if (precision > 0) {
                                sql.append(precision);
                            }
                            if (scale > 0) {
                                if (precision > 0) {
                                    sql.append(',');
                                }
                                sql.append(scale);
                            }
                            sql.append(')');
                        }
                    }
                    break;
                case DATETIME:
                    final int scale = CommonUtils.toInt(postgreColumn.getScale());
                    String typeName = dataType.getName();
                    if (typeName.startsWith(PostgreConstants.TYPE_TIMESTAMP) || typeName.equals(PostgreConstants.TYPE_TIME)) {
                        if (scale < 6) {
                            sql.append('(').append(scale).append(')');
                        }
                    }
                    if (typeName.equals(PostgreConstants.TYPE_INTERVAL)) {
                        final String precision = postgreColumn.getIntervalTypeField();
                        if (!CommonUtils.isEmpty(precision)) {
                            sql.append(' ').append(precision);
                        }
                        if (scale >= 0 && scale < 7) {
                            sql.append('(').append(scale).append(')');
                        }
                    }
            }
            if (PostgreUtils.isGISDataType(postgreColumn.getTypeName())) {
                try {
                    String geometryType = postgreColumn.getAttributeGeometryType(monitor);
                    int geometrySRID = postgreColumn.getAttributeGeometrySRID(monitor);
                    if (geometryType != null && !PostgreConstants.TYPE_GEOMETRY.equalsIgnoreCase(geometryType) && !PostgreConstants.TYPE_GEOGRAPHY.equalsIgnoreCase(geometryType)) {
                        // If data type is exactly GEOMETRY or GEOGRAPHY then it doesn't have qualifiers
                        sql.append("(").append(geometryType);
                        if (geometrySRID > 0) {
                            sql.append(", ").append(geometrySRID);
                        }
                        sql.append(")");
                    }
                } catch (DBCException e) {
                    log.debug(e);
                }
            }
            if (rawType != null) {
                sql.append("[]");
            }
            return sql;
        }
        return sql;
    }

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
        if (!CommonUtils.isEmpty(comment)) {
            sql.append(" -- ").append(CommonUtils.getSingleLineString(comment));
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
        ColumnModifier[] modifiers = {PostgreDataTypeModifier, NullNotNullModifier, PostgreIdentityModifier, PostgreCollateModifier};
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
        if (command.hasProperty(DBConstants.PROP_ID_DATA_TYPE) || command.hasProperty("maxLength") || command.hasProperty("precision") || command.hasProperty("scale")) {
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
    public void renameObject(DBECommandContext commandContext, PostgreTableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
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
