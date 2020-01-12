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

package org.jkiss.dbeaver.ext.vertica.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
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
public class VerticaTableColumnManager extends GenericTableColumnManager {

    protected final ColumnModifier<GenericTableColumn> VerticaDataTypeModifier = (monitor, column, sql, command) -> {
        sql.append(" SET DATA TYPE ");
        DataTypeModifier.appendModifier(monitor, column, sql, command);
    };

    protected final ColumnModifier<GenericTableColumn> VerticaDefaultModifier = (monitor, column, sql, command) -> {
        if (CommonUtils.isEmpty(command.getObject().getDefaultValue())) {
            sql.append(" DROP DEFAULT");
        } else {
            sql.append(" SET DEFAULT ");
            DefaultModifier.appendModifier(monitor, column, sql, command);
        }
    };

    protected final ColumnModifier<GenericTableColumn> VerticaNotNullModifier = (monitor, column, sql, command) -> {
        if (command.getObject().isRequired()) {
            sql.append(" SET NOT NULL");
        } else {
            sql.append(" DROP NOT NULL");
        }
    };

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, GenericTableBase owner, DBECommandAbstract<GenericTableColumn> command, Map<String, Object> options)
    {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        final GenericTableColumn column = command.getObject();
        if (column.isAutoIncrement()) {
            final String autoIncrementClause = column.getDataSource().getMetaModel().getAutoIncrementClause(column);
            if (autoIncrementClause != null && !autoIncrementClause.isEmpty()) {
                decl.append(" ").append(autoIncrementClause); //$NON-NLS-1$
            }
        }
        return decl;
    }

    @Override
    protected ColumnModifier[] getSupportedModifiers(GenericTableColumn column, Map<String, Object> options) {
        // According to SQL92 DEFAULT comes before constraints
        return new ColumnModifier[] {VerticaDataTypeModifier, VerticaDefaultModifier, VerticaNotNullModifier};
    }

    /**
     * Copy-pasted from PostgreSQL implementation.
     * TODO: Vertica is originally based on PG. Maybe we should refactor this stuff somehow.
     */
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
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

}
