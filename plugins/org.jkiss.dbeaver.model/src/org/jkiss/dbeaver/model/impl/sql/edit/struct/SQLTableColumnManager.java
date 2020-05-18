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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Table column manager. Fits for all composite entities including NoSQL.
 */
public abstract class SQLTableColumnManager<OBJECT_TYPE extends DBSEntityAttribute, TABLE_TYPE extends DBSEntity>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{
    public static final long DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP = 1;
    public static final long DDL_FEATURE_USER_BRACKETS_IN_DROP = 2;

    public static final String QUOTE = "'";

    protected interface ColumnModifier<OBJECT_TYPE extends DBPObject> {
        void appendModifier(DBRProgressMonitor monitor, OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command);
    }

    protected final ColumnModifier<OBJECT_TYPE> DataTypeModifier = (monitor, column, sql, command) -> {
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
    };

    protected final ColumnModifier<OBJECT_TYPE> NotNullModifier = (monitor, column, sql, command) -> {
        if (column.isRequired()) {
            sql.append(" NOT NULL"); //$NON-NLS-1$
        }
    };

    protected final ColumnModifier<OBJECT_TYPE> NullNotNullModifier = (monitor, column, sql, command) ->
        sql.append(column.isRequired() ? " NOT NULL" : " NULL");

    protected final ColumnModifier<OBJECT_TYPE> NullNotNullModifierConditional = (monitor, column, sql, command) -> {
        if (command instanceof DBECommandComposite) {
            if (((DBECommandComposite) command).getProperty("required") == null) {
                // Do not set NULL/NOT NULL if it wasn't changed
                return;
            }
        }
        NullNotNullModifier.appendModifier(monitor, column, sql, command);
    };

    protected final ColumnModifier<OBJECT_TYPE> DefaultModifier = (monitor, column, sql, command) -> {
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
    };

    protected ColumnModifier[] getSupportedModifiers(OBJECT_TYPE column, Map<String, Object> options)
    {
        return new ColumnModifier[] {DataTypeModifier, NotNullModifier, DefaultModifier};
    }

    @Override
    public boolean canEditObject(OBJECT_TYPE object)
    {
        DBSEntity table = object.getParentObject();
        return table != null && !DBUtils.isView(table);
    }

    @Override
    public boolean canCreateObject(Object container)
    {
        return container instanceof DBSTable && !((DBSTable) container).isView();
    }

    @Override
    public boolean canDeleteObject(OBJECT_TYPE object)
    {
        return canEditObject(object);
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
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
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final TABLE_TYPE table = (TABLE_TYPE) command.getObject().getParentObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_table_column,
                "ALTER TABLE " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) + " ADD "  + getNestedDeclaration(monitor, table, command, options)) );
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        boolean useBrackets = hasDDLFeature(command.getObject(), DDL_FEATURE_USER_BRACKETS_IN_DROP);
        StringBuilder ddl = new StringBuilder();
        ddl.append("ALTER TABLE ").append(DBUtils.getObjectFullName(command.getObject().getParentObject(), DBPEvaluationContext.DDL));
        ddl.append(" DROP ");
        if (useBrackets) ddl.append('(');
        if (!hasDDLFeature(command.getObject(), DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP)) {
            ddl.append("COLUMN ");
        }
        ddl.append(DBUtils.getQuotedIdentifier(command.getObject()));
        if (useBrackets) ddl.append(')');
        actions.add(
            new SQLDatabasePersistAction( ModelMessages.model_jdbc_drop_table_column, ddl.toString())
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
                        if (contextObject instanceof DBSEntityAttribute && ((DBSEntityAttribute) contextObject).getParentObject() == table && name.equalsIgnoreCase(((DBSEntityAttribute) contextObject).getName())) {
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
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
    {
        OBJECT_TYPE column = command.getObject();

        // Create column
        String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());

        if (command instanceof SQLObjectEditor.ObjectRenameCommand) {
            columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), ((ObjectRenameCommand) command).getNewName());
        }

        StringBuilder decl = new StringBuilder(40);
        decl.append(columnName);
        for (ColumnModifier<OBJECT_TYPE> modifier : getSupportedModifiers(column, options)) {
            modifier.appendModifier(monitor, column, decl, command);
        }

        return decl;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command, Map<String, Object> options)
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

