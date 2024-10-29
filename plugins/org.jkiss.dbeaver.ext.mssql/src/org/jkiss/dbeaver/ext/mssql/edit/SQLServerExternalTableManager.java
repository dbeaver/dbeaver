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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerExternalTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerSchema;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class SQLServerExternalTableManager extends SQLServerBaseTableManager<SQLServerExternalTable> {
    private static final Log log = Log.getLog(SQLServerExternalTableManager.class);

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        SQLServerTableColumn.class);

    @Override
    protected SQLServerExternalTable createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        final SQLServerSchema schema = (SQLServerSchema) container;
        final SQLServerExternalTable table = new SQLServerExternalTable(schema);
        setNewObjectName(monitor, schema, table);
        return table;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return false;
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, SQLServerExternalTable table, SQLObjectEditor.NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        try {
            final SQLServerExternalTable.AdditionalInfo info = table.getAdditionalInfo(monitor);
            ddl.append(" WITH (\n\tLOCATION = ").append(SQLUtils.quoteString(table, info.getExternalLocation()));
            ddl.append(",\n\tDATA_SOURCE = ").append(DBUtils.getQuotedIdentifier(table.getDataSource(), info.getExternalDataSource()));
            if (CommonUtils.isNotEmpty(info.getExternalFileFormat())) {
                ddl.append(",\n\tFILE_FORMAT = ").append(SQLUtils.quoteString(table, info.getExternalFileFormat()));
            }
            ddl.append("\n)");
        } catch (DBCException e) {
            log.error("Error retrieving external table info");
        }
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerExternalTable object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    protected String getCreateTableType(SQLServerExternalTable table) {
        return "EXTERNAL TABLE";
    }
}
