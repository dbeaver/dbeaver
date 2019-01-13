/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

    public enum Kind {
        TABLE,
        SEQUENCE,
        FUNCTION,
        COLUMN
    }

    private Kind kind;
    private String grantor;
    private String grantee;
    private String objectCatalog;
    private String objectSchema;
    private String objectName;
    private PostgrePrivilegeType privilegeType;
    private boolean isGrantable;
    private boolean withHierarchy;

    public PostgrePrivilege(Kind kind, ResultSet dbResult)
        throws SQLException
    {
        this.kind = kind;
        this.grantor = JDBCUtils.safeGetString(dbResult, "grantor");
        this.grantee = JDBCUtils.safeGetString(dbResult, "grantee");
        this.privilegeType = PostgrePrivilegeType.fromString(JDBCUtils.safeGetString(dbResult, "privilege_type"));
        this.isGrantable = JDBCUtils.safeGetBoolean(dbResult, "is_grantable");

        switch (kind) {
            case FUNCTION:
                this.objectCatalog = JDBCUtils.safeGetString(dbResult, "specific_catalog");
                this.objectSchema = JDBCUtils.safeGetString(dbResult, "specific_schema");
                this.objectName = JDBCUtils.safeGetString(dbResult, "specific_name");
                break;
            case SEQUENCE:
                this.objectCatalog = JDBCUtils.safeGetString(dbResult, "object_catalog");
                this.objectSchema = JDBCUtils.safeGetString(dbResult, "object_schema");
                this.objectName = JDBCUtils.safeGetString(dbResult, "object_name");
                break;
            default:
                this.objectCatalog = JDBCUtils.safeGetString(dbResult, "table_catalog");
                this.objectSchema = JDBCUtils.safeGetString(dbResult, "table_schema");
                this.objectName = JDBCUtils.safeGetString(dbResult, "table_name");
                this.withHierarchy = JDBCUtils.safeGetBoolean(dbResult, "with_hierarchy");
                break;
        }
    }

    public PostgrePrivilege(String grantor, String grantee, String objectCatalog, String objectSchema, String objectName, PostgrePrivilegeType privilegeType, boolean isGrantable, boolean withHierarchy) {
        this.grantor = grantor;
        this.grantee = grantee;
        this.objectCatalog = objectCatalog;
        this.objectSchema = objectSchema;
        this.objectName = objectName;
        this.privilegeType = privilegeType;
        this.isGrantable = isGrantable;
        this.withHierarchy = withHierarchy;
    }

    public Kind getKind() {
        return kind;
    }

    public String getGrantor() {
        return grantor;
    }

    public String getGrantee() {
        return grantee;
    }

    public String getObjectCatalog() {
        return objectCatalog;
    }

    public String getObjectSchema() {
        return objectSchema;
    }

    public String getObjectName() {
        return objectName;
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
