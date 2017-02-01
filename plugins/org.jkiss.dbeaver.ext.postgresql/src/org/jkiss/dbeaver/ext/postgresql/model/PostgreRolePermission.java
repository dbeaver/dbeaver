/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * PostgreRolePermission
 */
public class PostgreRolePermission extends PostgrePermission {

    private static final Log log = Log.getLog(PostgreRolePermission.class);

    private String schemaName;
    private String tableName;

    public PostgreRolePermission(PostgrePermissionsOwner owner, String schemaName, String tableName, List<PostgrePrivilege> privileges) {
        super(owner, privileges);
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getName() {
        return getFullTableName();
    }

    @Override
    public PostgreTableBase getTargetObject(DBRProgressMonitor monitor) throws DBException
    {
        final PostgreSchema schema = owner.getDatabase().getSchema(monitor, schemaName);
        if (schema != null) {
            return schema.getChild(monitor, tableName);
        }
        return null;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getFullTableName() {
        return DBUtils.getQuotedIdentifier(getDataSource(), schemaName) + "." +
            DBUtils.getQuotedIdentifier(getDataSource(), tableName);
    }

    @Override
    public String toString() {
        return getFullTableName();
    }

    @Override
    public int compareTo(@NotNull PostgrePermission o) {
        if (o instanceof PostgreRolePermission) {
            final int res = schemaName.compareTo(((PostgreRolePermission)o).schemaName);
            return res != 0 ? res : tableName.compareTo(((PostgreRolePermission)o).tableName);
        }
        return 0;
    }

}

