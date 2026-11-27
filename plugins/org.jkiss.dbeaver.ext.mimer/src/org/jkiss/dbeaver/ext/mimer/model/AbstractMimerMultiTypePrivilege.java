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
 * Shared shape for a privilege grant that carries a *selectable* privilege type per instance -
 * a grantee can hold more than one of them on the same owner (e.g. both {@code SELECT} and
 * {@code UPDATE} on a table) - unlike {@link AbstractMimerObjectPrivilege}, where the privilege
 * type is fixed per class. Since grantee alone isn't a unique identity here, {@link #getName()}
 * combines grantee + type, matching every subclass's own pre-existing convention. A subclass
 * only needs {@link #buildGrantRevokeTargetClause()} - whatever comes right after the privilege
 * type keyword, up to {@code TO}/{@code FROM} (e.g. {@code " ON DATABANK \"n\""}, or a column
 * grant's {@code "(\"col\") ON TABLE \"s\".\"t\""}) - everything else was previously duplicated
 * verbatim across {@code MimerObjectPrivilege}/{@code MimerDatabankPrivilege}/{@code
 * MimerColumnPrivilege}.
 *
 * @author Mimer Information Technology
 */
public abstract class AbstractMimerMultiTypePrivilege<O extends DBSObject> implements DBSObject, DBPSaveableObject, MimerGrantable {

    protected final O owner;
    private String grantee;
    private String privilegeType;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    protected AbstractMimerMultiTypePrivilege(@NotNull O owner, @NotNull JDBCResultSet dbResult) {
        this.owner = owner;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.privilegeType = JDBCUtils.safeGetStringTrimmed(dbResult, "PRIVILEGE_TYPE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    protected AbstractMimerMultiTypePrivilege(@NotNull O owner, @NotNull String grantee, @NotNull String privilegeType) {
        this.owner = owner;
        this.grantee = grantee;
        this.privilegeType = privilegeType;
        this.persisted = false;
    }

    /**
     * Whatever belongs right after the privilege type keyword in the DDL, up to {@code
     * TO}/{@code FROM} - most owners just need {@code " ON <KEYWORD> <qualified name>"}, but a
     * column-restricted grant needs the column name parenthesized immediately after the type
     * keyword first ({@code "(\"col\") ON TABLE ..."}), which is why this is a single opaque
     * string rather than separate keyword/name hooks the way {@link AbstractMimerObjectPrivilege}
     * splits them.
     */
    @NotNull
    protected abstract String buildGrantRevokeTargetClause();

    // Grantee alone isn't unique (the same grantee can hold several privilege types on the same
    // owner), so the identity/cache-key name combines both.
    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return grantee + " (" + privilegeType + ")";
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getGrantee() {
        return grantee;
    }

    public void setGrantee(@NotNull String grantee) {
        this.grantee = grantee;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getPrivilegeType() {
        return privilegeType;
    }

    public void setPrivilegeType(@NotNull String privilegeType) {
        this.privilegeType = privilegeType;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public String getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 5)
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

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) owner.getDataSource();
    }

    @NotNull
    @Override
    public final String buildGrantDDL() {
        String ddl = "GRANT " + privilegeType + buildGrantRevokeTargetClause() + " TO \"" + grantee.replace("\"", "\"\"") + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    @Override
    public final String buildRevokeDDL() {
        return "REVOKE " + privilegeType + buildGrantRevokeTargetClause() + " FROM \"" + grantee.replace("\"", "\"\"") + "\"";
    }
}
