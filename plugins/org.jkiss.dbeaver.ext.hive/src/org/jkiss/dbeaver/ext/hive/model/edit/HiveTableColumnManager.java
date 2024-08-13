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
package org.jkiss.dbeaver.ext.hive.model.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.hive.model.HiveTable;
import org.jkiss.dbeaver.ext.hive.model.HiveTableColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.util.List;
import java.util.Map;

/**
 * HiveTableColumnManager
 */
public class HiveTableColumnManager extends GenericTableColumnManager {

    private static final Log log = Log.getLog(HiveTableColumnManager.class);

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(@NotNull GenericTableColumn object) {
        return true;
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        HiveTable table = (HiveTable) command.getObject().getParentObject();
        actions.add(
                new SQLDatabasePersistAction(
                        "Add table column",
                        "ALTER TABLE " + DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) + " ADD COLUMNS ("  +
                                getNestedDeclaration(monitor, table, command, options) + ")") );
    }

    @Override
    public StringBuilder getNestedDeclaration(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase owner, @NotNull DBECommandAbstract<GenericTableColumn> command, @NotNull Map<String, Object> options) {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        return decl;
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) throws DBException {
        HiveTableColumn hiveTableColumn = (HiveTableColumn) command.getObject();
        HiveTable table = (HiveTable) hiveTableColumn.getParentObject();
        try {
            List<? extends GenericTableColumn> attributes = table.getAttributes(monitor);
            //It may not be the best option. Some of the column data may still be lost. It might be worth using a temporary table
            StringBuilder ddl = new StringBuilder();
            ddl.append("ALTER TABLE ").append(DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL)).append(" REPLACE COLUMNS (");
            if (attributes != null) {
                for (int i = 0; i < attributes.size(); i++) {
                    GenericTableColumn column = attributes.get(i);
                    if (column != hiveTableColumn) {
                        if (i != 0) {
                            ddl.append(" ");
                        }
                        ddl.append(DBUtils.getQuotedIdentifier(column)).append(" ").append(column.getTypeName());
                        String typeModifiers = SQLUtils.getColumnTypeModifiers(table.getDataSource(), column, column.getTypeName(), column.getDataKind());
                        if (typeModifiers != null) {
                            ddl.append(typeModifiers);
                        }
                        String description = column.getDescription();
                        if (column.getDescription() != null) {
                            ddl.append(" COMMENT '").append(description).append("'");
                        }
                        if (i != attributes.size() - 1) {
                            ddl.append(",");
                        }
                    }
                }
            }
            ddl.append(")");
            actions.add(new SQLDatabasePersistAction("Drop table column", ddl.toString()));
        } catch (DBException e) {
            log.debug("Columns not found in table: " + table.getName(), e);
        }
    }

}
