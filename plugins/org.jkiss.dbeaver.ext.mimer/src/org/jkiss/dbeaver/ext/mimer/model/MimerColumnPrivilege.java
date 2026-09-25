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
 * One {@code INSERT}/{@code UPDATE}/{@code REFERENCES} privilege grant restricted to a single
 * table/view column, read from the SQL-standard {@code INFORMATION_SCHEMA.COLUMN_PRIVILEGES}
 * view (see {@link PrivilegeCache}) - the same view {@link MimerIdentColumnPrivilege} already
 * reads, just filtered by column instead of by grantee. {@code SELECT} is deliberately excluded
 * from {@link #PRIVILEGE_TYPES} - a column-restricted grant only ever covers {@code
 * INSERT|REFERENCES|UPDATE}, narrowing what you can insert/update/reference, not what you can
 * read - Mimer SQL has no column-restricted {@code SELECT}. Shared by
 * {@link MimerTableColumn} for both table and view columns, same "one DDL keyword covers both"
 * reasoning as {@link MimerObjectPrivilege}.
 *
 * @author Mimer Information Technology
 */
public class MimerColumnPrivilege implements DBSObject, DBPSaveableObject {

    public static final String[] PRIVILEGE_TYPES = {"INSERT", "UPDATE", "REFERENCES"};

    private final MimerTableColumn column;
    private String grantee;
    private String privilegeType;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerColumnPrivilege(@NotNull MimerTableColumn column, @NotNull JDBCResultSet dbResult) {
        this.column = column;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.privilegeType = JDBCUtils.safeGetString(dbResult, "PRIVILEGE_TYPE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerColumnPrivilege(@NotNull MimerTableColumn column, @NotNull String grantee, @NotNull String privilegeType) {
        this.column = column;
        this.grantee = grantee;
        this.privilegeType = privilegeType;
        this.persisted = false;
    }

    // Grantee alone isn't unique per column (the same grantee can hold several privilege
    // types), so the identity/cache-key name combines both - same convention as MimerObjectPrivilege.
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
        return column;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) column.getDataSource();
    }

    @NotNull
    public MimerTableColumn getColumn() {
        return column;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT " + privilegeType + "(\"" + column.getName() + "\") ON TABLE \""
            + column.getTable().getSchema().getName() + "\".\"" + column.getTable().getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE " + privilegeType + "(\"" + column.getName() + "\") ON TABLE \""
            + column.getTable().getSchema().getName() + "\".\"" + column.getTable().getName() + "\" FROM \"" + grantee + "\"";
    }

    /**
     * Shared by every {@link MimerTableColumn} (table or view) - each column owns its own
     * instance. {@code GRANTOR = '_SYSTEM'} rows excluded, same reasoning as {@link
     * MimerObjectPrivilege.PrivilegeCache}.
     */
    public static class PrivilegeCache extends JDBCObjectCache<MimerTableColumn, MimerColumnPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerTableColumn owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.COLUMN_PRIVILEGES\n" +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ? AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE, PRIVILEGE_TYPE");
            stmt.setString(1, owner.getTable().getSchema().getName());
            stmt.setString(2, owner.getTable().getName());
            stmt.setString(3, owner.getName());
            return stmt;
        }

        @Override
        protected MimerColumnPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerTableColumn owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerColumnPrivilege(owner, resultSet);
        }
    }
}
