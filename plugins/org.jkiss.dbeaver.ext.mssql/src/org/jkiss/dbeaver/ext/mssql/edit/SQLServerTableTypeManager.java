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
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class SQLServerTableTypeManager extends SQLServerBaseTableManager<SQLServerTableType> {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
            SQLServerTableColumn.class,
            SQLServerTableUniqueKey.class,
            SQLServerTableForeignKey.class,
            SQLServerTableIndex.class,
            SQLServerTableCheckConstraint.class
        );

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerTableType object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        throw new DBException("SQL Server data table types rename not supported");
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    protected SQLServerTableType createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    protected String beginCreateTableStatement(DBRProgressMonitor monitor, SQLServerTableType table, String tableName, Map<String, Object> options) throws DBException {
        if (!options.isEmpty() && options.containsKey(DBPScriptObject.OPTION_USE_SPECIAL_NAME)) {
            return "CREATE TYPE " + options.get(DBPScriptObject.OPTION_USE_SPECIAL_NAME) + " AS TABLE (\n";
        }
        return "CREATE TYPE " + tableName + " AS TABLE\n (";
    }

    @Override
    protected boolean isIncludeDropInDDL(@NotNull SQLServerTableType tableType) {
        return false;
    }
}
