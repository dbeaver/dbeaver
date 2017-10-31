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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * JDBC table column manager
 */
public abstract class SQLTableColumnManager<OBJECT_TYPE extends JDBCTableColumn<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{
    public static final long DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP = 1;
    public static final String QUOTE = "'";

    protected interface ColumnModifier<OBJECT_TYPE extends DBPObject> {
        void appendModifier(OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command);
    }

    protected final ColumnModifier<OBJECT_TYPE> DataTypeModifier = new ColumnModifier<OBJECT_TYPE>() {
        @Override
        public void appendModifier(OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command) {
            final String typeName = column.getTypeName();
            DBPDataKind dataKind = column.getDataKind();
            final DBSDataType dataType = findDataType(column.getDataSource(), typeName);
            sql.append(' ').append(typeName);
            if (dataType == null) {
                log.debug("Type name '" + typeName + "' is not supported by driver"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                dataKind = dataType.getDataKind();
            }
            String modifiers = SQLUtils.getColumnTypeModifiers(column.getDataSource(), column, typeName, dataKind);
            if (modifiers != null) {
                sql.append(modifiers);
            }
        }
    };

    protected final ColumnModifier<OBJECT_TYPE> NotNullModifier = new ColumnModifier<OBJECT_TYPE>() {
        @Override
        public void appendModifier(OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command) {
            if (column.isRequired()) {
                sql.append(" NOT NULL"); //$NON-NLS-1$
            }
        }
    };

    protected final ColumnModifier<OBJECT_TYPE> NullNotNullModifier = new ColumnModifier<OBJECT_TYPE>() {
        @Override
        public void appendModifier(OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command) {
            sql.append(column.isRequired() ? " NOT NULL" : " NULL");
        }
    };

    protected final ColumnModifier<OBJECT_TYPE> NullNotNullModifierConditional = new ColumnModifier<OBJECT_TYPE>() {
        @Override
        public void appendModifier(OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command) {
            if (command instanceof DBECommandComposite) {
                if (((DBECommandComposite) command).getProperty("required") == null) {
                    // Do not set NULL/NOT NULL if it wasn't chaged
                    return;
                }
            }
            NullNotNullModifier.appendModifier(column, sql, command);
        }
    };

    protected final ColumnModifier<OBJECT_TYPE> DefaultModifier = new ColumnModifier<OBJECT_TYPE>() {
        @Override
        public void appendModifier(OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command) {
            String defaultValue = CommonUtils.toString(column.getDefaultValue());
            if (!CommonUtils.isEmpty(defaultValue)) {
                DBPDataKind dataKind = column.getDataKind();
                boolean useQuotes = false;//dataKind == DBPDataKind.STRING;
                if (!defaultValue.startsWith(QUOTE) && !defaultValue.endsWith(QUOTE)) {
                    if (useQuotes && defaultValue.trim().startsWith(QUOTE)) {
                        useQuotes = false;
                    }
                    if (dataKind == DBPDataKind.DATETIME) {
                        final char firstChar = defaultValue.trim().charAt(0);
                        if (!Character.isLetter(firstChar) && firstChar != '(' && firstChar != '[') {
                            useQuotes = true;
                        }
                    }
                }

                sql.append(" DEFAULT "); //$NON-NLS-1$
                if (useQuotes) sql.append(QUOTE);
                sql.append(defaultValue);
                if (useQuotes) sql.append(QUOTE);
            }
        }
    };


    protected ColumnModifier[] getSupportedModifiers(OBJECT_TYPE column)
    {
        return new ColumnModifier[] {DataTypeModifier, NotNullModifier, DefaultModifier};
    }

    @Override
    public boolean canEditObject(OBJECT_TYPE object)
    {
        TABLE_TYPE table = object.getParentObject();
        return table != null && !table.isView();
    }

    @Override
    public boolean canCreateObject(TABLE_TYPE parent)
    {
        return parent != null && !parent.isView();
    }

    @Override
    public boolean canDeleteObject(OBJECT_TYPE object)
    {
        return canEditObject(object);
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected long getDDLFeatures(OBJECT_TYPE object)
    {
        return 0;
    }

    private boolean hasDDLFeature(OBJECT_TYPE object, long feature)
    {
        return (getDDLFeatures(object) & feature) != 0;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_table_column,
                "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD "  + getNestedDeclaration(table, command, options)) );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table_column, "ALTER TABLE " + command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-2$
                    " DROP " + (hasDDLFeature(command.getObject(), DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP) ? "" : "COLUMN ") + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        );
    }

    protected String getNewColumnName(DBRProgressMonitor monitor, DBECommandContext context, TABLE_TYPE table)
    {
        for (int i = 1; ; i++)  {
            final String name = DBObjectNameCaseTransformer.transformName(table.getDataSource(), "Column" + i);
            try {
                // check for existing columns
                boolean exists = table.getAttribute(monitor, name) != null;
                if (!exists) {
                    // Check for new columns (they are present only within command context)
                    for (DBPObject contextObject : context.getEditedObjects()) {
                        if (contextObject instanceof JDBCTableColumn && ((JDBCTableColumn) contextObject).getTable() == table && name.equalsIgnoreCase(((JDBCTableColumn) contextObject).getName())) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    return name;
                }
            } catch (DBException e) {
                log.warn(e);
                return name;
            }
        }

    }

    @Override
    protected StringBuilder getNestedDeclaration(TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
    {
        OBJECT_TYPE column = command.getObject();

        // Create column
        String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());

        if (command instanceof SQLObjectEditor.ObjectRenameCommand) {
            columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), ((ObjectRenameCommand) command).getNewName());
        }

        StringBuilder decl = new StringBuilder(40);
        decl.append(columnName);
        for (ColumnModifier<OBJECT_TYPE> modifier : getSupportedModifiers(column)) {
            modifier.appendModifier(column, decl, command);
        }

        return decl;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Column name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getTypeName())) {
            throw new DBException("Column type name cannot be empty");
        }
    }

    private static DBSDataType findDataType(DBPDataSource dataSource, String typeName)
    {
        if (dataSource instanceof DBPDataTypeProvider) {
            return ((DBPDataTypeProvider) dataSource).getLocalDataType(typeName);
        }
        return null;
    }

    protected static DBSDataType findBestDataType(DBPDataSource dataSource, String ... typeNames)
    {
        if (dataSource instanceof DBPDataTypeProvider) {
            return DBUtils.findBestDataType(((DBPDataTypeProvider) dataSource).getLocalDataTypes(), typeNames);
        }
        return null;
    }

}

