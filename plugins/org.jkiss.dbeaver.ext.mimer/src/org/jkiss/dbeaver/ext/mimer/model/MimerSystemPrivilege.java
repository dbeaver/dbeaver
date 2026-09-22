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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * A system-level (not per-object) privilege granted to a Mimer SQL ident, read from {@code
 * INFORMATION_SCHEMA.EXT_SYSTEM_PRIVILEGES WHERE GRANTEE = ?}. Reused unchanged across {@link
 * MimerUser}/{@link MimerGroup}/{@link MimerProgram}'s own "System Privileges" folders (each with
 * its own {@code SystemPrivilegeCache} wrapper - Java generics don't allow one cache class to
 * serve three unrelated owner types, same reason every other per-ident privilege class in this
 * plugin is hand-duplicated per owner rather than sharing one cache).
 * <p>
 * Create/drop support (see {@link org.jkiss.dbeaver.ext.mimer.edit.MimerSystemPrivilegeManager}):
 * {@code GRANT <privilege> TO "ident" [WITH GRANT OPTION]} / {@code REVOKE <privilege> FROM
 * "ident"}. The six grantable privilege types ({@code BACKUP}/{@code DATABANK}/
 * {@code IDENT}/{@code SCHEMA}/{@code SHADOW}/{@code STATISTICS}) come from Mimer SQL's own
 * fixed value list for this statement. No {@code CASCADE} on revoke -
 * omitting it matches the "RESTRICT is the implicit default"
 * convention every other privilege-revoke manager here follows (Group Member, Object Privilege,
 * Routine Privilege, ...).
 *
 * @author Mimer Information Technology
 */
public class MimerSystemPrivilege implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    private final DBSObject ident;
    private String privilegeType;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerSystemPrivilege(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        this.ident = ident;
        this.privilegeType = JDBCUtils.safeGetStringTrimmed(dbResult, "PRIVILEGE_TYPE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerSystemPrivilege(@NotNull DBSObject ident, @NotNull String privilegeType) {
        this.ident = ident;
        this.privilegeType = privilegeType;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return privilegeType;
    }

    @Override
    public void setName(String name) {
        this.privilegeType = name;
    }

    @Property(viewable = true, order = 2)
    public String getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 3)
    public boolean isGrantable() {
        return grantable;
    }

    public void setGrantable(boolean grantable) {
        this.grantable = grantable;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
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

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT " + privilegeType + " TO \"" + ident.getName() + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE " + privilegeType + " FROM \"" + ident.getName() + "\"";
    }
}
