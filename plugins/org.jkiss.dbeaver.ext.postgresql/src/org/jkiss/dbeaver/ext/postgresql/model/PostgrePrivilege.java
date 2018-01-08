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

    public PostgrePrivilege(String grantor, String grantee, String tableCatalog, String tableSchema, String tableName, PostgrePrivilegeType privilegeType, boolean isGrantable, boolean withHierarchy) {
        this.grantor = grantor;
        this.grantee = grantee;
        this.tableCatalog = tableCatalog;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
        this.privilegeType = privilegeType;
        this.isGrantable = isGrantable;
        this.withHierarchy = withHierarchy;
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

