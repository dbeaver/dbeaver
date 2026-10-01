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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

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
 * reasoning as {@link MimerObjectPrivilege}. See {@link AbstractMimerMultiTypePrivilege} for the
 * shape this and every other selectable-privilege-type grant class share.
 *
 * @author Mimer Information Technology
 */
public class MimerColumnPrivilege extends AbstractMimerMultiTypePrivilege<MimerTableColumn> {

    public static final String[] PRIVILEGE_TYPES = {"INSERT", "UPDATE", "REFERENCES"};

    public MimerColumnPrivilege(@NotNull MimerTableColumn column, @NotNull JDBCResultSet dbResult) {
        super(column, dbResult);
    }

    public MimerColumnPrivilege(@NotNull MimerTableColumn column, @NotNull String grantee, @NotNull String privilegeType) {
        super(column, grantee, privilegeType);
    }

    @NotNull
    public MimerTableColumn getColumn() {
        return owner;
    }

    @NotNull
    @Override
    protected String buildGrantRevokeTargetClause() {
        return "(\"" + owner.getName().replace("\"", "\"\"") + "\") ON TABLE \""
            + owner.getTable().getSchema().getName().replace("\"", "\"\"")
            + "\".\"" + owner.getTable().getName().replace("\"", "\"\"") + "\"";
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
