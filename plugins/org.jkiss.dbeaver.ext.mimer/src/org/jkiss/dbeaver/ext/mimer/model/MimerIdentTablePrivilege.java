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
 * One table/view privilege granted <b>to</b> a Mimer SQL ident (User/Program/Group), read from
 * {@code INFORMATION_SCHEMA.TABLE_PRIVILEGES WHERE GRANTEE = ?} - the reverse direction of {@link
 * MimerObjectPrivilege} (which shows grants on one table, this shows grants to one ident, across
 * every table). Backs a per-ident "Access rights" summary, reading the same view {@link
 * MimerObjectPrivilege} itself reads, just filtered differently. Read-only summary -
 * grant/revoke stays on the table's own Privileges folder, this is purely "what does this
 * ident have, across everything".
 *
 * @author Mimer Information Technology
 */
public class MimerIdentTablePrivilege extends AbstractMimerIdentPrivilegeSummary {

    private final String tableSchema;
    private final String tableName;
    private final String privilegeType;

    public MimerIdentTablePrivilege(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        super(ident, dbResult);
        this.tableSchema = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEMA");
        this.tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.privilegeType = JDBCUtils.safeGetString(dbResult, "PRIVILEGE_TYPE");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return tableSchema + "." + tableName + " (" + privilegeType + ")";
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
    public String getPrivilegeType() {
        return privilegeType;
    }
}
