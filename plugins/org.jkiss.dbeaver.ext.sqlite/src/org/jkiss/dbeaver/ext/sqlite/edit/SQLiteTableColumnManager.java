/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQLiteTableColumnManager
 */
public class SQLiteTableColumnManager extends GenericTableColumnManager
    implements DBEObjectRenamer<GenericTableColumn>
{

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(GenericTableColumn object) {
        return true;
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        final GenericTableColumn column = command.getObject();
        final GenericTableBase table = column.getTable();

        final String tableName = DBUtils.getQuotedIdentifier(table);
        final String tableColumns;
        final String tableDDL;

        final List<? extends GenericTableColumn> attributes = table.getAttributes(monitor);
        if (CommonUtils.isEmpty(attributes)) {
            throw new DBException("Table has no attributes");
        }
        try {
            if (attributes.contains(column)) {
                table.removeAttribute(column);
            }
            tableColumns = attributes.stream()
                .filter(DBPPersistedObject::isPersisted)
                .map(DBUtils::getQuotedIdentifier)
                .collect(Collectors.joining(",\n  "));
            tableDDL = DBStructUtils.generateTableDDL(monitor, table, Collections.emptyMap(), false);
        } finally {
            if (attributes.contains(column)) {
                table.addAttribute(column);
            }
        }

        actions.add(new SQLDatabasePersistActionComment(
            table.getDataSource(),
            "Drop column " + DBUtils.getQuotedIdentifier(column)
        ));
        actions.add(new SQLDatabasePersistAction(
            "Create temporary table from original table",
            "CREATE TEMPORARY TABLE temp AS\nSELECT\n  " + tableColumns + "\nFROM " + tableName
        ));
        actions.add(new SQLDatabasePersistAction(
            "Drop original table",
            "\nDROP TABLE " + tableName + ";\n"
        ));
        actions.add(new SQLDatabasePersistAction(
            "Create new table",
            tableDDL
        ));
        actions.add(new SQLDatabasePersistAction(
            "Insert values from temporary table to new table",
            "INSERT INTO " + tableName + "\n (" + tableColumns + ")\nSELECT\n  " + tableColumns + "\nFROM temp"
        ));
        actions.add(new SQLDatabasePersistAction(
            "Drop temporary table",
            "\nDROP TABLE temp"
        ));
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final GenericTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(column.getTable()) + " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                    " TO " + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull GenericTableColumn object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

}
