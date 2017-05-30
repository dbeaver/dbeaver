/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.postgresql.model.*;
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
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.AttributeEditPage;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.List;

/**
 * Postgre table column manager
 */
public class PostgreTableColumnManager extends SQLTableColumnManager<PostgreTableColumn, PostgreTableBase> implements DBEObjectRenamer<PostgreTableColumn>  {

    protected final ColumnModifier<PostgreTableColumn> PostgreDataTypeModifier = new ColumnModifier<PostgreTableColumn>() {
        @Override
        public void appendModifier(PostgreTableColumn column, StringBuilder sql, DBECommandAbstract<PostgreTableColumn> command) {
            sql.append(' ');
            final PostgreDataType dataType = column.getDataType();
            final PostgreDataType rawType = dataType.getElementType();
            if (rawType != null) {
                sql.append(rawType.getTypeName());
            } else {
                sql.append(dataType.getTypeName());
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
                        final int precision = column.getPrecision();
                        final int scale = column.getScale();
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
        }
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTableColumn> getObjectsCache(PostgreTableColumn object)
    {
        return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier[] getSupportedModifiers(PostgreTableColumn column)
    {
        return new ColumnModifier[] {PostgreDataTypeModifier, NullNotNullModifier, DefaultModifier};
    }

    @Override
    public StringBuilder getNestedDeclaration(PostgreTableBase owner, DBECommandAbstract<PostgreTableColumn> command)
    {
        StringBuilder decl = super.getNestedDeclaration(owner, command);
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
                final PostgreDataType dataType = parent.getDatabase().getDataType(PostgreOid.VARCHAR);
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
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
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
        if (command.getProperty("dataType") != null || command.getProperty("maxLength") != null || command.getProperty("precision") != null || command.getProperty("scale") != null) {
            actionList.add(new SQLDatabasePersistAction("Set column type", prefix + "TYPE " + typeClause));
        }
        if (command.getProperty("required") != null) {
            actionList.add(new SQLDatabasePersistAction("Set column nullability", prefix + (column.isRequired() ? "SET" : "DROP") + " NOT NULL"));
        }

        if (command.getProperty("defaultValue") != null) {
            if (CommonUtils.isEmpty(column.getDefaultValue())) {
                actionList.add(new SQLDatabasePersistAction("Drop column default", prefix + "DROP DEFAULT"));
            } else {
                actionList.add(new SQLDatabasePersistAction("Set column default", prefix + "SET DEFAULT " + column.getDefaultValue()));
            }
        }
        if (command.getProperty("description") != null) {
            actionList.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
                DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                " IS " + SQLUtils.quoteString(column, CommonUtils.notEmpty(column.getDescription()))));
        }
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
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
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
