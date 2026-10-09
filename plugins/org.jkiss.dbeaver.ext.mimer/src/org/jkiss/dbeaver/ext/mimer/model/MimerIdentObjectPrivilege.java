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
public class MimerIdentObjectPrivilege extends AbstractMimerIdentPrivilegeSummary {

    private final String objectSchema;
    private final String objectName;
    private final String objectType;
    private final String privilegeType;

    public MimerIdentObjectPrivilege(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        super(ident, dbResult);
        this.objectSchema = JDBCUtils.safeGetString(dbResult, "OBJECT_SCHEMA");
        this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.objectType = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_TYPE");
        this.privilegeType = JDBCUtils.safeGetStringTrimmed(dbResult, "PRIVILEGE_TYPE");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return objectName + " (" + objectType + "/" + privilegeType + ")";
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getObjectSchema() {
        return objectSchema;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getObjectName() {
        return objectName;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public String getObjectType() {
        return objectType;
    }

    @NotNull
    @Property(viewable = true, order = 5)
    public String getPrivilegeType() {
        return privilegeType;
    }
}
