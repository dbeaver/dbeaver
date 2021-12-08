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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.dbeaver.ext.mssql.model.SQLServerExternalTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

public class SQLServerExternalTableManager extends SQLServerTableManager {
    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, SQLServerTableBase table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        final SQLServerExternalTable externalTable = (SQLServerExternalTable) table;

        ddl.append(" WITH (\n\tLOCATION = ").append(SQLUtils.quoteString(table, externalTable.getExternalLocation()));
        ddl.append(",\n\tDATA_SOURCE = ").append(DBUtils.getQuotedIdentifier(table.getDataSource(), externalTable.getExternalDataSource()));
        if (CommonUtils.isNotEmpty(externalTable.getExternalFileFormat())) {
            ddl.append(",\n\tFILE_FORMAT = ").append(SQLUtils.quoteString(table, externalTable.getExternalFileFormat()));
        }
        ddl.append("\n)");
    }

    @Override
    protected String getCreateTableType(SQLServerTableBase table) {
        return "EXTERNAL TABLE";
    }
}
