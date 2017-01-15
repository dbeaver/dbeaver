/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

