/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * One column-restricted privilege granted to a Mimer SQL ident (e.g. {@code GRANT INSERT (col1)
 * ON TABLE ...}), read from {@code INFORMATION_SCHEMA.COLUMN_PRIVILEGES WHERE GRANTEE = ?}.
 * This is the one place in this plugin that can show a
 * column-restricted grant at all - {@link MimerObjectPrivilege}'s own per-table Privileges folder
 * can't, since {@code EXT_OBJECT_PRIVILEGES}/{@code TABLE_PRIVILEGES} don't carry column
 * information. Read-only summary, same as {@link MimerIdentTablePrivilege}.
 *
 * @author Mimer Information Technology
 */
public class MimerIdentColumnPrivilege extends AbstractMimerIdentPrivilegeSummary {

    private final String tableSchema;
    private final String tableName;
    private final String columnName;
    private final String privilegeType;

    public MimerIdentColumnPrivilege(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        super(ident, dbResult);
        this.tableSchema = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEMA");
        this.tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.privilegeType = JDBCUtils.safeGetString(dbResult, "PRIVILEGE_TYPE");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return tableSchema + "." + tableName + "." + columnName + " (" + privilegeType + ")";
    }

    @Property(viewable = true, order = 2)
    public String getTableSchema() {
        return tableSchema;
    }

    @Property(viewable = true, order = 3)
    public String getTableName() {
        return tableName;
    }

    @Property(viewable = true, order = 4)
    public String getColumnName() {
        return columnName;
    }

    @Property(viewable = true, order = 5)
    public String getPrivilegeType() {
        return privilegeType;
    }
}
