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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;

/**
 * One {@code SELECT}/{@code INSERT}/{@code UPDATE}/{@code DELETE}/{@code REFERENCES} privilege
 * grant on a table or view, read from the SQL-standard {@code
 * INFORMATION_SCHEMA.TABLE_PRIVILEGES} view (see {@link PrivilegeCache}), shared by {@link
 * MimerTable}/{@link MimerView} since Mimer SQL's GRANT/REVOKE syntax addresses both with {@code ON
 * TABLE} - there's no separate {@code ON VIEW} form.
 * <p>
 * <b>Not {@code EXT_OBJECT_PRIVILEGES}</b>: that view does not surface grants to a {@code
 * PROGRAM} ident, while {@code TABLE_PRIVILEGES} does, along with Mimer SQL's own {@code
 * _SYSTEM}-granted implicit owner rows. A per-ident "Access rights" summary (on a
 * Program/User/Group) reads this same {@code TABLE_PRIVILEGES} view filtered by {@code GRANTEE}
 * instead of by table. {@code REFERENCES} is a real grantable type; {@code ALL PRIVILEGES} is a
 * grant/revoke shorthand for all of them at once (offered in the dialog, but the catalog stores
 * no such row - a granted {@code ALL} reads back as the individual rows); {@code LOAD}, seen only
 * on the {@code _SYSTEM}-granted rows, is not user-grantable and is left out of {@link
 * #PRIVILEGE_TYPES}.
 * <p>
 * Column-level grants ({@code GRANT INSERT (col1, col2) ON TABLE ...}) are not modeled - neither
 * view exposes which columns a column-restricted grant covers, so only whole-object grants are
 * supported.
 *
 * @author Mimer Information Technology
 */
public class MimerObjectPrivilege implements DBSObject, DBPSaveableObject {

    // "ALL PRIVILEGES" is a create-only shorthand for GRANT/REVOKE - the catalog never stores a
    // row of that type, so a granted ALL reads back as the individual SELECT/INSERT/... rows.
    public static final String[] PRIVILEGE_TYPES = {"SELECT", "INSERT", "UPDATE", "DELETE", "REFERENCES", "ALL PRIVILEGES"};

    private final GenericTableBase table;
    private String grantee;
    private String privilegeType;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerObjectPrivilege(@NotNull GenericTableBase table, @NotNull JDBCResultSet dbResult) {
        this.table = table;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.privilegeType = JDBCUtils.safeGetString(dbResult, "PRIVILEGE_TYPE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerObjectPrivilege(@NotNull GenericTableBase table, @NotNull String grantee, @NotNull String privilegeType) {
        this.table = table;
        this.grantee = grantee;
        this.privilegeType = privilegeType;
        this.persisted = false;
    }

    // Grantee alone isn't unique per table (the same grantee can hold several privilege
    // types), so the identity/cache-key name combines both.
    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return grantee + " (" + privilegeType + ")";
    }

    @Property(viewable = true, order = 2)
    public String getGrantee() {
        return grantee;
    }

    public void setGrantee(String grantee) {
        this.grantee = grantee;
    }

    @Property(viewable = true, order = 3)
    public String getPrivilegeType() {
        return privilegeType;
    }

    public void setPrivilegeType(String privilegeType) {
        this.privilegeType = privilegeType;
    }

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

    @Override
    public DBSObject getParentObject() {
        return table;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) table.getDataSource();
    }

    @NotNull
    public GenericTableBase getTable() {
        return table;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT " + privilegeType + " ON TABLE \"" + table.getSchema().getName() + "\".\"" + table.getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE " + privilegeType + " ON TABLE \"" + table.getSchema().getName() + "\".\"" + table.getName() + "\" FROM \"" + grantee + "\"";
    }

    /**
     * Shared by {@link MimerTable}/{@link MimerView} - each owns its own instance. Reads {@code
     * TABLE_PRIVILEGES}, not {@code EXT_OBJECT_PRIVILEGES} - see the class Javadoc for why.
     * {@code GRANTOR = '_SYSTEM'} rows are Mimer SQL's own synthetic "the owner implicitly has every
     * privilege" entries, not real grants, and are excluded since there's nothing a normal
     * {@code REVOKE} could act on for one of those.
     */
    public static class PrivilegeCache extends JDBCObjectCache<GenericTableBase, MimerObjectPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericTableBase owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES\n" +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE, PRIVILEGE_TYPE");
            stmt.setString(1, owner.getSchema().getName());
            stmt.setString(2, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectPrivilege fetchObject(@NotNull JDBCSession session, @NotNull GenericTableBase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectPrivilege(owner, resultSet);
        }
    }
}
