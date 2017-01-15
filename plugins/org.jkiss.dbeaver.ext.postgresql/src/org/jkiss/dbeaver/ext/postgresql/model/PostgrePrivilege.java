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

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgrePrivilege
 */
public class PostgrePrivilege {

    private String grantor;
    private String grantee;
    private String tableCatalog;
    private String tableSchema;
    private String tableName;
    private PostgrePrivilegeType privilegeType;
    private boolean isGrantable;
    private boolean withHierarchy;

    public PostgrePrivilege(ResultSet dbResult)
        throws SQLException
    {
        this.grantor = JDBCUtils.safeGetString(dbResult, "grantor");
        this.grantee = JDBCUtils.safeGetString(dbResult, "grantee");
        this.tableCatalog = JDBCUtils.safeGetString(dbResult, "table_catalog");
        this.tableSchema = JDBCUtils.safeGetString(dbResult, "table_schema");
        this.tableName = JDBCUtils.safeGetString(dbResult, "table_name");
        this.privilegeType = PostgrePrivilegeType.fromString(JDBCUtils.safeGetString(dbResult, "privilege_type"));
        this.isGrantable = JDBCUtils.safeGetBoolean(dbResult, "is_grantable");
        this.withHierarchy = JDBCUtils.safeGetBoolean(dbResult, "with_hierarchy");
    }

    public String getGrantor() {
        return grantor;
    }

    public String getGrantee() {
        return grantee;
    }

    public String getTableCatalog() {
        return tableCatalog;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    public String getTableName() {
        return tableName;
    }

    public PostgrePrivilegeType getPrivilegeType() {
        return privilegeType;
    }

    public boolean isGrantable() {
        return isGrantable;
    }

    public boolean isWithHierarchy() {
        return withHierarchy;
    }

    @Override
    public String toString() {
        return privilegeType.toString();
    }
}

