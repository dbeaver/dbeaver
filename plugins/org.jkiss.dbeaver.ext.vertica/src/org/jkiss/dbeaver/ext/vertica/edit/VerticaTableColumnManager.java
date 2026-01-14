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

package org.jkiss.dbeaver.ext.vertica.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Vertica table column manager
 */
public class VerticaTableColumnManager extends GenericTableColumnManager implements DBEObjectRenamer<GenericTableColumn> {

    private final ColumnModifier<GenericTableColumn> VerticaDataTypeModifier = (monitor, column, sql, command) -> {
        sql.append(" SET DATA TYPE ");
        DataTypeModifier.appendModifier(monitor, column, sql, command);
    };

    private final ColumnModifier<GenericTableColumn> VerticaDefaultModifier = (monitor, column, sql, command) -> {
        if (CommonUtils.isEmpty(command.getObject().getDefaultValue())) {
            sql.append(" DROP DEFAULT");
        } else {
            sql.append(" SET DEFAULT ");
            DefaultModifier.appendModifier(monitor, column, sql, command);
        }
    };

    private final ColumnModifier<GenericTableColumn> VerticaNotNullModifier = (monitor, column, sql, command) -> {
        if (command.getObject().isRequired()) {
            sql.append(" SET NOT NULL");
        } else {
            sql.append(" DROP NOT NULL");
        }
    };

    private final ColumnModifier<GenericTableColumn> IncrementModifier = (monitor, column, sql, command) -> {
        if (column.isAutoIncrement()) {
            sql.append(" IDENTITY"); //$NON-NLS-1$
        }
    };

    @Override
    protected ColumnModifier[] getSupportedModifiers(GenericTableColumn column, Map<String, Object> options) {
        if (column.isAutoIncrement() && !column.isPersisted()) {
            // DefaultModifier and DataTypeModifier not supported in this case, IncrementModifier must be before NotNullModifier
            return new ColumnModifier[] {IncrementModifier, NotNullModifier};
        }
        if (column.isPersisted()) {
            // According to SQL92 DEFAULT comes before constraints
            return new ColumnModifier[]{VerticaDataTypeModifier, VerticaDefaultModifier, VerticaNotNullModifier};
        }
        return super.getSupportedModifiers(column, options);
    }

    @Override
    public void addIncrementClauseToNestedDeclaration(DBECommandAbstract<GenericTableColumn> command, StringBuilder decl) {
        // Increment clause already append with the IncrementModifier
    }

    /**
     * Copy-pasted from PostgreSQL implementation.
     * TODO: Vertica is originally based on PG. Maybe we should refactor this stuff somehow.
     */
    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException
    {
        final GenericTableColumn column = command.getObject();

        String prefix = "ALTER TABLE " + DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + " ALTER COLUMN " + DBUtils.getQuotedIdentifier(column) + " ";
        String typeClause = column.getFullTypeName();
        if (command.getProperty(DBConstants.PROP_ID_TYPE_NAME) != null || command.getProperty("maxLength") != null || command.getProperty("precision") != null || command.getProperty("scale") != null) {
            actionList.add(new SQLDatabasePersistAction("Set column type", prefix + "SET DATA TYPE " + typeClause));
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
        super.addObjectModifyActions(monitor, executionContext, actionList, command, options);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options)
    {
        final GenericTableColumn column = command.getObject();

        actions.add(
                new SQLDatabasePersistAction(
                        "Rename column",
                        "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME COLUMN " +
                                DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                                " TO " + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull GenericTableColumn object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }
}
