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
package org.jkiss.dbeaver.ext.clickhouse.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTable;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Clickhouse table manager
 */
public class ClickhouseTableManager extends GenericTableManager {

    private static final Log log = Log.getLog(ClickhouseTableManager.class);

    @Override
    protected String getDropTableType(GenericTableBase table) {
        // Both tables and views must be deleted with DROP TABLE
        return "TABLE";
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, GenericTableBase table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        if (table instanceof ClickhouseTable) {
            ClickhouseTable clickhouseTable = (ClickhouseTable) table;
            if (clickhouseTable.getEngine() != null) {
                ddl.append(" ENGINE = ").append(clickhouseTable.getEngine().getName());
                if (CommonUtils.isNotEmpty(clickhouseTable.getEngineMessage())) {
                    ddl.append("\n").append(clickhouseTable.getEngineMessage());
                }
            } else {
                try {
                    List<? extends GenericTableColumn> attributes = table.getAttributes(monitor);
                    if (!CommonUtils.isEmpty(attributes)) {
                        ddl.append(" ENGINE = MergeTree()\n" +
                            "ORDER BY ").append(DBUtils.getQuotedIdentifier(attributes.get(0)));
                    } else {
                        ddl.append(" ENGINE = Log");
                    }
                } catch (DBException e) {
                    log.debug("Can't read " + table.getName() + " columns");
                }
            }
            if (!table.isPersisted() && tableProps.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null
                && CommonUtils.isNotEmpty(table.getDescription())) {
                ddl.append("\nCOMMENT ").append(SQLUtils.quoteString(table, table.getDescription())); //$NON-NLS-1$
            }
        }
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<GenericTableBase, PropertyHandler> command,
        @NotNull Map<String, Object> options)
    {
        GenericTableBase tableBase = command.getObject();
        if (tableBase.isPersisted() && command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "ALTER TABLE " + tableBase.getFullyQualifiedName(DBPEvaluationContext.DDL)
                    + " MODIFY COMMENT "
                    + SQLUtils.quoteString(tableBase, CommonUtils.notEmpty(tableBase.getDescription()))));
        }
    }
}
