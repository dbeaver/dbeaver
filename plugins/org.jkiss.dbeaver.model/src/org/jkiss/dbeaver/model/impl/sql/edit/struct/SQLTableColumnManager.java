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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectWithDependencies;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Table column manager. Fits for all composite entities including NoSQL.
 */
public abstract class SQLTableColumnManager<OBJECT_TYPE extends DBSEntityAttribute, TABLE_TYPE extends DBSEntity>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE> implements DBEObjectWithDependencies
{
    public static final long DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP = 1;
    public static final long DDL_FEATURE_USER_BRACKETS_IN_DROP = 2;
    public static final long FEATURE_ALTER_TABLE_ADD_COLUMN = 4;

    public static final String QUOTE = "'";

    protected interface ColumnModifier<OBJECT_TYPE extends DBPObject> {
        void appendModifier(DBRProgressMonitor monitor, OBJECT_TYPE column, StringBuilder sql, DBECommandAbstract<OBJECT_TYPE> command);
    }

    protected final ColumnModifier<OBJECT_TYPE> DataTypeModifier = (monitor, column, sql, command) -> {
        final String typeName = column.getTypeName();
        DBPDataKind dataKind = column.getDataKind();
        final DBSDataType dataType = findDataType(column, typeName);
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
        if (column.isPersisted() && command instanceof DBECommandComposite) {
            if (((DBECommandComposite) command).getProperty("required") == null) {
                // Do not set NULL/NOT NULL if it wasn't changed
                return;
            }
        }
        NullNotNullModifier.appendModifier(monitor, column, sql, command);
    };

    protected ColumnModifier<OBJECT_TYPE> DefaultModifier = new BaseDefaultModifier();

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
    public boolean canCreateObject(@NotNull Object container)
    {
        return container instanceof DBSTable && !((DBSTable) container).isView();
    }

    @Override
    public boolean canDeleteObject(@NotNull OBJECT_TYPE object)
    {
        return canEditObject(object);
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
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
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        final TABLE_TYPE table = (TABLE_TYPE) command.getObject().getParentObject();
        StringBuilder sql = new StringBuilder(256);
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL)).append(" ADD ");
        if (hasDDLFeature(command.getObject(), FEATURE_ALTER_TABLE_ADD_COLUMN)) {
            sql.append("COLUMN ");
        }
        sql.append(getNestedDeclaration(monitor, table, command, options));
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_table_column,
                sql.toString()) );
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException
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

    @NotNull
    protected String getNewColumnName(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, @NotNull TABLE_TYPE table) {
        return DBUtils.makeNewObjectName(monitor, "Column{0}", table, DBSEntityAttribute.class, DBSEntity::getAttribute, context);
    }

    @Override
    protected StringBuilder getNestedDeclaration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TABLE_TYPE owner,
        @NotNull DBECommandAbstract<OBJECT_TYPE> command,
        @NotNull Map<String, Object> options
    ) {
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
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Column name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getTypeName())) {
            throw new DBException("Column type name cannot be empty");
        }
    }

    private static DBSDataType findDataType(DBSObject object, String typeName)
    {
        DBPDataTypeProvider dataTypeProvider = DBUtils.getParentOfType(DBPDataTypeProvider.class, object);
        if (dataTypeProvider != null) {
            return dataTypeProvider.getLocalDataType(typeName);
        }
        return null;
    }

    protected static DBSDataType findBestDataType(DBSObject object, String ... typeNames)
    {
        DBPDataTypeProvider dataTypeProvider = DBUtils.getParentOfType(DBPDataTypeProvider.class, object);
        if (dataTypeProvider != null) {
            return DBUtils.findBestDataType(dataTypeProvider.getLocalDataTypes(), typeNames);
        }
        return null;
    }

    @Override
    public List<? extends DBSObject> getDependentObjectsList(DBRProgressMonitor monitor, DBSObject object) throws DBException {
        DBSObject dbsObject = object.getParentObject();
        Set<DBSObject> dependentObjectsList = new HashSet<>();
        if (dbsObject instanceof DBSEntity && object instanceof DBSEntityAttribute) {
            DBSEntity parentObject = (DBSEntity) dbsObject;

            Collection<? extends DBSEntityConstraint> constraints = parentObject.getConstraints(monitor);
            if (!CommonUtils.isEmpty(constraints)) {
                for (DBSEntityConstraint constraint : constraints) {
                    addDependentConstraints(monitor, (DBSEntityAttribute) object, dependentObjectsList, constraint);
                }
            }

            Collection<? extends DBSEntityAssociation> associations = parentObject.getAssociations(monitor);
            if (!CommonUtils.isEmpty(associations)) {
                for (DBSEntityAssociation association : associations) {
                    addDependentConstraints(monitor, (DBSEntityAttribute) object, dependentObjectsList, association);
                }
            }
        }

        if (dbsObject instanceof DBSTable) {
            Collection<? extends DBSTableIndex> indexes = ((DBSTable) dbsObject).getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexes)) {
                for (DBSTableIndex index : indexes) {
                    List<? extends DBSTableIndexColumn> attributeReferences = index.getAttributeReferences(monitor);
                    if (!CommonUtils.isEmpty(attributeReferences)) {
                        for (DBSTableIndexColumn indexColumn : attributeReferences) {
                            DBSTableColumn tableColumn = indexColumn.getTableColumn();
                            if (tableColumn == object) {
                                dependentObjectsList.add(index);
                                break;
                            }
                        }
                    }
                }
            }

        }
        return new ArrayList<>(dependentObjectsList);
    }

    private void addDependentConstraints(DBRProgressMonitor monitor, DBSEntityAttribute object, Set<DBSObject> dependentObjectsList, DBSObject constraint) throws DBException {
        if (constraint instanceof DBSEntityReferrer) {
            List<? extends DBSEntityAttributeRef> attributeReferences = ((DBSEntityReferrer) constraint).getAttributeReferences(monitor);
            if (!CommonUtils.isEmpty(attributeReferences)) {
                for (DBSEntityAttributeRef attributeRef : attributeReferences) {
                    if (attributeRef.getAttribute() == object) {
                        dependentObjectsList.add(constraint);
                        break;
                    }
                }
            }
        }
    }

    public static void addColumnCommentAction(List<DBEPersistAction> actionList, DBSEntityAttribute column, DBSEntity table) {
        actionList.add(new SQLDatabasePersistAction(
            "Comment column",
            "COMMENT ON COLUMN " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                " IS " + SQLUtils.quoteString(column.getDataSource(), CommonUtils.notEmpty(column.getDescription()))));
    }

    protected class BaseDefaultModifier implements ColumnModifier<OBJECT_TYPE> {
        @Override
        public void appendModifier(
            @NotNull DBRProgressMonitor monitor,
            @NotNull OBJECT_TYPE column,
            @NotNull StringBuilder sql,
            @NotNull DBECommandAbstract<OBJECT_TYPE> command
        ) {
            String defaultValue = CommonUtils.toString(column.getDefaultValue());
            if (!CommonUtils.isEmpty(defaultValue)) {
                DBPDataKind dataKind = column.getDataKind();
                boolean useQuotes = isUsesQuotes(defaultValue, dataKind);
                sql.append(" DEFAULT "); //$NON-NLS-1$
                appendDefaultValue(sql, defaultValue, useQuotes);
            }
        }

        protected boolean isUsesQuotes(@NotNull String defaultValue, @NotNull DBPDataKind dataKind) {
            if (!defaultValue.startsWith(QUOTE) && !defaultValue.endsWith(QUOTE)) {
                if (dataKind == DBPDataKind.STRING || dataKind == DBPDataKind.DATETIME) {
                    final String trimmed = defaultValue.trim();
                    if (trimmed.isEmpty()) {
                        return true;
                    }
                    final char firstChar = trimmed.charAt(0);
                    return !Character.isLetter(firstChar) && firstChar != '(' && firstChar != '[';
                }
            }
            return false;
        }

        protected void appendDefaultValue(@NotNull StringBuilder sql, @NotNull String defaultValue, boolean useQuotes) {
            if (useQuotes) {
                sql.append(QUOTE);
            }
            sql.append(defaultValue);
            if (useQuotes) {
                sql.append(QUOTE);
            }
        }

    }

}
