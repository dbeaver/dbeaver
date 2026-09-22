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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * One non-table privilege granted <b>to</b> a Mimer SQL ident, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES WHERE GRANTEE = ?}. Backs the "Object Privileges" tab
 * only ({@code PRIVILEGE_TYPE IN ('EXECUTE','SEQUENCE','TABLE','USAGE')} - this deliberately
 * excludes SELECT/INSERT/UPDATE/DELETE, already covered by {@link MimerIdentTablePrivilege}'s
 * own "Access rights" folder, and folds a Program's dedicated "Execute" tab into this one too,
 * since it's the identical data (EXECUTE is one of the covered types) just scoped to one program
 * instead of every object). Read-only summary - real grant/revoke stays on each object's own
 * Privileges folder (matching every other object type this union covers, none of which share one
 * uniform DDL shape - unlike group membership).
 * <p>
 * The {@code PRIVILEGE_TYPE = 'MEMBER'} case ("Group memberships" tab) is modeled separately by
 * {@link MimerIdentGroupMembership}, which has real create/drop support - {@code MEMBER} is the
 * one privilege type in this union with a single, uniform DDL shape ({@code GRANT/REVOKE MEMBER
 * ON GROUP ...}), matching the write support {@link MimerGroupMember} already has from the
 * group's own side.
 *
 * @author Mimer Information Technology
 */
public class MimerIdentObjectPrivilege implements DBSObject {

    private final DBSObject ident;
    private final String objectSchema;
    private final String objectName;
    private final String objectType;
    private final String privilegeType;
    private final String grantor;
    private final boolean grantable;

    public MimerIdentObjectPrivilege(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        this.ident = ident;
        this.objectSchema = JDBCUtils.safeGetString(dbResult, "OBJECT_SCHEMA");
        this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.objectType = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_TYPE");
        this.privilegeType = JDBCUtils.safeGetStringTrimmed(dbResult, "PRIVILEGE_TYPE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return objectName + " (" + objectType + "/" + privilegeType + ")";
    }

    @Property(viewable = true, order = 2)
    public String getObjectSchema() {
        return objectSchema;
    }

    @Property(viewable = true, order = 3)
    public String getObjectName() {
        return objectName;
    }

    @Property(viewable = true, order = 4)
    public String getObjectType() {
        return objectType;
    }

    @Property(viewable = true, order = 5)
    public String getPrivilegeType() {
        return privilegeType;
    }

    @Property(viewable = true, order = 6)
    public String getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 7)
    public boolean isGrantable() {
        return grantable;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return ident;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) ident.getDataSource();
    }
}
