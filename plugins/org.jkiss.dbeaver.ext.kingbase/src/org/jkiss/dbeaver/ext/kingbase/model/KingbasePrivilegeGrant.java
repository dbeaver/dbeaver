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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * KingbasePrivilegeGrant
 */
public class KingbasePrivilegeGrant {

    public enum Kind {
        SCHEMA,
        TABLE,
        SEQUENCE,
        FUNCTION,
        PROCEDURE,
        COLUMN,
        TYPE
    }

    private Kind kind;
    private KingbaseRoleReference grantor;
    private KingbaseRoleReference grantee;
    private String objectCatalog;
    private String objectSchema;
    private String objectName;
    private KingbasePrivilegeType privilegeType;
    private boolean isGrantable;
    private boolean withHierarchy;

    public KingbasePrivilegeGrant(
        @NotNull KingbaseDatabase database,
        @NotNull Kind kind,
        @NotNull ResultSet dbResult
    ) throws SQLException {
        this.kind = kind;
        this.grantor = obtainRoleReference(database, dbResult, "grantor");
        this.grantee = obtainRoleReference(database, dbResult, "grantee");
        this.privilegeType = KingbasePrivilegeType.fromString(JDBCUtils.safeGetString(dbResult, "privilege_type"));
        this.isGrantable = JDBCUtils.safeGetBoolean(dbResult, "is_grantable");

        switch (kind) {
            case PROCEDURE:
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

    private static KingbaseRoleReference obtainRoleReference(KingbaseDatabase database, ResultSet dbResult, String columnName) {
        String roleName = JDBCUtils.safeGetString(dbResult, columnName);
        return roleName == null ? null : new KingbaseRoleReference(database, roleName, null);
    }

    public KingbasePrivilegeGrant(KingbaseRoleReference grantor, KingbaseRoleReference grantee, String objectCatalog, String objectSchema, String objectName, KingbasePrivilegeType privilegeType, boolean isGrantable, boolean withHierarchy) {
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

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public KingbaseRoleReference getGrantor() {
        return grantor;
    }

    public KingbaseRoleReference getGrantee() {
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

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public KingbasePrivilegeType getPrivilegeType() {
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
