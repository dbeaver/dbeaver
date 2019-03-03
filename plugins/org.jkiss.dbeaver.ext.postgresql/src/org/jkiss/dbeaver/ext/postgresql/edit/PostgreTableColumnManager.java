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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.AttributeEditPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Postgre table column manager
 */
public class PostgreTableColumnManager extends SQLTableColumnManager<PostgreTableColumn, PostgreTableBase> implements DBEObjectRenamer<PostgreTableColumn>  {

    protected final ColumnModifier<PostgreTableColumn> PostgreDataTypeModifier = (monitor, column, sql, command) -> {
        sql.append(' ');
        final PostgreDataType dataType = column.getDataType();
        String defValue = column.getDefaultValue();
        if (!CommonUtils.isEmpty(defValue) && defValue.contains("nextval")) {
            // Use serial type name
            switch (dataType.getName()) {
                case PostgreConstants.TYPE_INT2:
                    sql.append("smallserial");
                    return;
                case PostgreConstants.TYPE_INT4:
                    sql.append("serial");
                    return;
                case PostgreConstants.TYPE_INT8:
                    sql.append("bigserial");
                    return;
            }
        }
        final PostgreDataType rawType = dataType.getElementType(monitor);
        if (rawType != null) {
            sql.append(rawType.getFullyQualifiedName(DBPEvaluationContext.DDL));
        } else {
            sql.append(dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
        }
        switch (dataType.getDataKind()) {
            case STRING:
                final long length = column.getMaxLength();
                if (length > 0) {
                    sql.append('(').append(length).append(')');
                }
                break;
            case NUMERIC:
                if (dataType.getTypeID() == Types.NUMERIC) {
                    final int precision = CommonUtils.toInt(column.getPrecision());
                    final int scale = CommonUtils.toInt(column.getScale());
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
        }
        if (rawType != null) {
            sql.append("[]");
        }
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreDefaultModifier = (monitor, column, sql, command) -> {
        String defaultValue = column.getDefaultValue();
        if (!CommonUtils.isEmpty(defaultValue) && defaultValue.contains("nextval")) {
            // Use serial type name
            switch (column.getDataType().getName()) {
                case PostgreConstants.TYPE_INT2:
                case PostgreConstants.TYPE_INT4:
                case PostgreConstants.TYPE_INT8:
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
            if (collation != null) {
                sql.append(" COLLATE ").append(collation.getName());
            }
        } catch (DBException e) {
            log.debug(e);
        }
    };

    protected final ColumnModifier<PostgreTableColumn> PostgreCommentModifier = (monitor, column, sql, command) -> {
        String comment = column.getDescription();
        if (!CommonUtils.isEmpty(comment)) {
            sql.append(" -- ").append(TextUtils.getSingleLineString(comment));
        }
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTableColumn> getObjectsCache(PostgreTableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier[] getSupportedModifiers(PostgreTableColumn column, Map<String, Object> options)
    {
        ColumnModifier[] modifiers = {PostgreDataTypeModifier, NullNotNullModifier, PostgreDefaultModifier, PostgreIdentityModifier, PostgreCollateModifier};
        if (CommonUtils.getOption(options, PostgreConstants.OPTION_DDL_SHOW_COLUMN_COMMENTS)) {
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
    protected PostgreTableColumn createDatabaseObject(final DBRProgressMonitor monitor, final DBECommandContext context, final PostgreTableBase parent, Object copyFrom)
    {
        return new UITask<PostgreTableColumn>() {
            @Override
            protected PostgreTableColumn runTask() {
                final PostgreTableColumn column = new PostgreTableColumn(parent);
                column.setName(getNewColumnName(monitor, context, parent));
                final PostgreDataType dataType = parent.getDatabase().getDataType(monitor, PostgreOid.VARCHAR);
                column.setDataType(dataType); //$NON-NLS-1$
                column.setOrdinalPosition(-1);

                AttributeEditPage page = new AttributeEditPage(null, column);
                if (!page.edit()) {
                    return null;
                }
                // Varchar length doesn't make much sense for PG
//                if (column.getDataKind() == DBPDataKind.STRING && !column.getTypeName().contains("text") && column.getMaxLength() <= 0) {
//                    column.setMaxLength(100);
//                }
                return column;
            }
        }.execute();

    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        super.addObjectCreateActions(monitor, actions, command, options);
        if (!CommonUtils.isEmpty(command.getObject().getDescription())) {
            addColumnCommentAction(actions, command.getObject());
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final PostgreAttribute column = command.getObject();
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
        if (column.getDataType() != null) {
            typeClause += " USING " + DBUtils.getQuotedIdentifier(column) + "::" + column.getDataType().getName();
        }
        if (command.getProperty(DBConstants.PROP_ID_DATA_TYPE) != null || command.getProperty("maxLength") != null || command.getProperty("precision") != null || command.getProperty("scale") != null) {
            actionList.add(new SQLDatabasePersistAction("Set column type", prefix + "TYPE " + typeClause));
        }
        if (command.getProperty(DBConstants.PROP_ID_REQUIRED) != null) {
            actionList.add(new SQLDatabasePersistAction("Set column nullability", prefix + (column.isRequired() ? "SET" : "DROP") + " NOT NULL"));
        }

        if (command.getProperty(DBConstants.PROP_ID_DEFAULT_VALUE) != null) {
            if (CommonUtils.isEmpty(column.getDefaultValue())) {
                actionList.add(new SQLDatabasePersistAction("Drop column default", prefix + "DROP DEFAULT"));
            } else {
                actionList.add(new SQLDatabasePersistAction("Set column default", prefix + "SET DEFAULT " + column.getDefaultValue()));
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
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final PostgreAttribute column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

}
