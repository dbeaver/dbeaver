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
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * One object privilege grant on a databank - {@code GRANT TABLE|SEQUENCE ON DATABANK "<db>" TO
 * "<ident>"} lets the grantee create tables / sequences in that databank. Read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} ({@code OBJECT_TYPE = 'DATABANK'}, {@code
 * OBJECT_SCHEMA} = the databank's creator), see {@link PrivilegeCache} - same shape as {@link
 * AbstractMimerMultiTypePrivilege} in general, and {@link MimerObjectPrivilege} specifically (a
 * grantee can hold both {@code TABLE} and {@code SEQUENCE}).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankPrivilege extends AbstractMimerMultiTypePrivilege<MimerDatabank> {

    public static final String[] PRIVILEGE_TYPES = {"TABLE", "SEQUENCE"};

    public MimerDatabankPrivilege(@NotNull MimerDatabank databank, @NotNull JDBCResultSet dbResult) {
        super(databank, dbResult);
    }

    public MimerDatabankPrivilege(@NotNull MimerDatabank databank, @NotNull String grantee, @NotNull String privilegeType) {
        super(databank, grantee, privilegeType);
    }

    @NotNull
    public MimerDatabank getDatabank() {
        return owner;
    }

    @NotNull
    @Override
    protected String buildGrantRevokeTargetClause() {
        return " ON DATABANK \"" + owner.getName().replace("\"", "\"\"") + "\"";
    }

    /**
     * Reads {@code EXT_OBJECT_PRIVILEGES} for {@code OBJECT_TYPE = 'DATABANK'} - only ever shows
     * grants where the connected ident is GRANTOR or GRANTEE (same caveat as every other
     * {@code EXT_OBJECT_PRIVILEGES}-backed privilege list in this plugin). Two kinds of noise are
     * filtered out: {@code GRANTOR = '_SYSTEM'} (Mimer SQL's synthetic "the owner implicitly
     * holds TABLE/SEQUENCE" rows, same as the table-privilege {@code _SYSTEM} filter) and, when
     * the creator is known, {@code GRANTEE = <creator>} (any further grant to the owner is
     * redundant - the owner already has it). {@code OBJECT_SCHEMA} is scoped to the creator,
     * matching DbVisualizer's own {@code mimer.getObjectPrivileges}.
     */
    public static class PrivilegeCache extends JDBCObjectCache<MimerDatabank, MimerDatabankPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDatabank owner) throws SQLException {
            String creator = CommonUtils.trim(owner.getCreator());
            boolean haveCreator = !CommonUtils.isEmpty(creator);
            String sql =
                "SELECT GRANTEE, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_NAME = ? AND OBJECT_TYPE = 'DATABANK'\n" +
                "  AND PRIVILEGE_TYPE IN ('TABLE', 'SEQUENCE')\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                (haveCreator ? "  AND OBJECT_SCHEMA = ? AND GRANTEE <> ?\n" : "") +
                "ORDER BY GRANTEE, PRIVILEGE_TYPE";
            JDBCPreparedStatement stmt = session.prepareStatement(sql);
            stmt.setString(1, owner.getName());
            if (haveCreator) {
                stmt.setString(2, creator);
                stmt.setString(3, creator);
            }
            return stmt;
        }

        @Override
        protected MimerDatabankPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerDatabank owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerDatabankPrivilege(owner, resultSet);
        }
    }
}
