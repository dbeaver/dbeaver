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
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * One object privilege grant on a databank - {@code GRANT TABLE|SEQUENCE ON DATABANK "<db>" TO
 * "<ident>"} lets the grantee create tables / sequences in that databank. Read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} ({@code OBJECT_TYPE = 'DATABANK'}, {@code
 * OBJECT_SCHEMA} = the databank's creator), see {@link PrivilegeCache} - same two-field-identity
 * shape as {@link MimerObjectPrivilege} (a grantee can hold both {@code TABLE} and {@code
 * SEQUENCE}).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankPrivilege implements DBSObject, DBPSaveableObject {

    public static final String[] PRIVILEGE_TYPES = {"TABLE", "SEQUENCE"};

    private final MimerDatabank databank;
    private String grantee;
    private String privilegeType;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerDatabankPrivilege(@NotNull MimerDatabank databank, @NotNull JDBCResultSet dbResult) {
        this.databank = databank;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.privilegeType = JDBCUtils.safeGetStringTrimmed(dbResult, "PRIVILEGE_TYPE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerDatabankPrivilege(@NotNull MimerDatabank databank, @NotNull String grantee, @NotNull String privilegeType) {
        this.databank = databank;
        this.grantee = grantee;
        this.privilegeType = privilegeType;
        this.persisted = false;
    }

    // Grantee alone isn't unique (the same grantee can hold both TABLE and SEQUENCE), so the
    // identity/cache-key name combines both.
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
        return databank;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) databank.getDataSource();
    }

    @NotNull
    public MimerDatabank getDatabank() {
        return databank;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT " + privilegeType + " ON DATABANK \"" + databank.getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE " + privilegeType + " ON DATABANK \"" + databank.getName() + "\" FROM \"" + grantee + "\"";
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
