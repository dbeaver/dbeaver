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
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * One group this ident belongs to - the reverse direction of {@link MimerGroupMember} (which
 * shows a group's members; this shows which groups an ident is a member of), backed by the same
 * underlying {@code EXT_OBJECT_PRIVILEGES} {@code MEMBER} row, read via each owner type's own
 * {@code GroupMembershipCache} (one per owner class, same reasoning as every other per-ident
 * privilege class in this plugin - Java generics don't allow one cache class to serve
 * {@code MimerUser}/{@code MimerGroup}/{@code MimerProgram} at once).
 * <p>
 * Split out from the read-only {@link MimerIdentObjectPrivilege} (which still backs the "Object
 * Privileges" tab) so this folder - unlike Access Rights/Column Privileges/Object Privileges,
 * which stay pure read-only summaries pointing back at the real per-object UI - can offer real
 * create/drop support as a same-DDL, reverse-direction convenience alongside the group's own
 * "Members" folder.
 *
 * @author Mimer Information Technology
 */
public class MimerIdentGroupMembership implements DBSObject, DBPSaveableObject {

    private final DBSObject ident;
    private String groupName;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerIdentGroupMembership(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        this.ident = ident;
        this.groupName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerIdentGroupMembership(@NotNull DBSObject ident, @NotNull String groupName) {
        this.ident = ident;
        this.groupName = groupName;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
    public DBSObject getIdent() {
        return ident;
    }

    /**
     * Same DDL as {@link MimerGroupMember#buildGrantDDL()}, just issued from the member's own
     * side rather than the group's.
     */
    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT MEMBER ON GROUP \"" + groupName + "\" TO \"" + ident.getName() + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE MEMBER ON GROUP \"" + groupName + "\" FROM \"" + ident.getName() + "\"";
    }
}
