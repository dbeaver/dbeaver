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
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A Mimer SQL PROGRAM ident, read from {@code INFORMATION_SCHEMA.EXT_IDENTS} ({@code
 * IDENT_TYPE = 'PROGRAM'}, see {@link MimerDataSource.ProgramCache}) - a separate ident kind
 * used for the {@code ENTER <program> USING <password>} security layer, not a database
 * principal that connects directly (see {@link MimerConstants#PROP_PROGRAM}/{@code
 * PROP_PROGRAM_PASSWORD}, already used by the connection wizard's Advanced page). Unlike
 * {@link MimerUser}, a program has no schema and no OS login - per the docs, its password is
 * mandatory (not optional) and capped at 18 characters.
 *
 * @author Mimer Information Technology
 */
public class MimerProgram implements DBSObject, DBPNamedObject2, DBPSaveableObject, DBPRefreshableObject {

    private final MimerDataSource dataSource;
    private String name;
    private String creator;
    private boolean hasPassword;
    private boolean persisted;

    // CREATE-only; also reused post-creation to drive ALTER IDENT SET PASSWORD. Always blank on
    // load - EXT_IDENTS only says whether a password is set, never its value.
    private String password;

    private String comment;
    private final PrivilegeCache privilegeCache = new PrivilegeCache();
    private final TablePrivilegeCache tablePrivilegeCache = new TablePrivilegeCache();
    private final ColumnPrivilegeCache columnPrivilegeCache = new ColumnPrivilegeCache();
    private final ObjectPrivilegeCache objectPrivilegeCache = new ObjectPrivilegeCache();
    private final GroupMembershipCache groupMembershipCache = new GroupMembershipCache();
    private final SystemPrivilegeCache systemPrivilegeCache = new SystemPrivilegeCache();

    public MimerProgram(@NotNull MimerDataSource dataSource, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "IDENT_NAME");
        this.creator = JDBCUtils.safeGetString(dbResult, "IDENT_CREATOR");
        this.hasPassword = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "HAS_PASSWORD"));
        this.persisted = true;
    }

    public MimerProgram(@NotNull MimerDataSource dataSource, @NotNull String name) {
        this.dataSource = dataSource;
        this.name = name;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 2)
    public String getCreator() {
        return creator;
    }

    @Property(viewable = true, order = 3)
    public boolean isHasPassword() {
        return hasPassword;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 4, password = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON IDENT "name" IS '...'} - Users, Groups, and Programs are all {@code
     * IDENT}s in Mimer SQL's comment statement and in {@code EXT_OBJECT_IDENT_USAGE.OBJECT_TYPE},
     * same as {@link MimerUser#getComment}.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 5)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, null, null, name, "IDENT");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    @Association
    public Collection<MimerProgramPrivilege> getPrivileges(DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerProgram, MimerProgramPrivilege> getPrivilegeCache() {
        return privilegeCache;
    }

    /**
     * Schemas owned by this program - always empty in practice (a program has no schema, unlike
     * a user), but included for parity with the per-ident "Schemas" folder on User/Group, which
     * shows the identical (always-empty) view for a program too.
     */
    @Association
    public List<MimerSchema> getOwnedSchemas(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<MimerSchema> result = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read Mimer SQL schemas owned by program")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_OWNER = ?")
            ) {
                dbStat.setString(1, name);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String schemaName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
                        MimerSchema schema = (MimerSchema) dataSource.getSchema(schemaName);
                        if (schema != null) {
                            result.add(schema);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
        return result;
    }

    /**
     * Table/view privileges granted to this program, across every table - see {@link
     * MimerIdentTablePrivilege}.
     */
    @Association
    public Collection<MimerIdentTablePrivilege> getTablePrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tablePrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Column-restricted privileges granted to this program - see {@link
     * MimerIdentColumnPrivilege}.
     */
    @Association
    public Collection<MimerIdentColumnPrivilege> getColumnPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return columnPrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Non-table object privileges (EXECUTE/SEQUENCE/TABLE/USAGE) granted to this program - see
     * {@link MimerIdentObjectPrivilege}. This is the reverse direction from {@link
     * #getPrivileges} (what this program itself can do to other objects, vs. who can EXECUTE
     * this program) - also covers what would otherwise be a separate "Execute" tab, since EXECUTE
     * is one of the privilege types included here.
     */
    @Association
    public Collection<MimerIdentObjectPrivilege> getObjectPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objectPrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Groups this program is a member of.
     */
    @Association
    public Collection<MimerIdentGroupMembership> getGroupMemberships(@NotNull DBRProgressMonitor monitor) throws DBException {
        return groupMembershipCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerProgram, MimerIdentGroupMembership> getGroupMembershipCache() {
        return groupMembershipCache;
    }

    /**
     * System-level (not per-object) privileges granted to this program - see {@link
     * MimerSystemPrivilege}.
     */
    @Association
    public Collection<MimerSystemPrivilege> getSystemPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return systemPrivilegeCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerProgram, MimerSystemPrivilege> getSystemPrivilegeCache() {
        return systemPrivilegeCache;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        privilegeCache.clearCache();
        tablePrivilegeCache.clearCache();
        columnPrivilegeCache.clearCache();
        objectPrivilegeCache.clearCache();
        groupMembershipCache.clearCache();
        systemPrivilegeCache.clearCache();
        comment = null;
        return this;
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
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return dataSource;
    }

    /**
     * {@code CREATE IDENT "name" AS PROGRAM USING 'password'} - unlike {@link MimerUser}, the
     * password is mandatory, never omitted.
     */
    @NotNull
    public String buildCreateDDL() {
        return "CREATE IDENT \"" + name + "\" AS PROGRAM USING '" + CommonUtils.notEmpty(password).replace("'", "''") + "'";
    }

    @NotNull
    public String buildDropDDL() {
        return "DROP IDENT \"" + name + "\"";
    }

    /**
     * {@code ALTER IDENT "name" SET PASSWORD '...'} - the only alterable property.
     */
    @NotNull
    public List<String> buildAlterDDL(@NotNull Set<String> changedProperties) {
        List<String> statements = new ArrayList<>(1);
        if (changedProperties.contains("password") && !CommonUtils.isEmpty(password)) {
            statements.add("ALTER IDENT \"" + name + "\" SET PASSWORD '" + password.replace("'", "''") + "'");
        }
        return statements;
    }

    /**
     * {@code EXECUTE} grants on this program - see {@link MimerProgramPrivilege}.
     */
    /**
     * {@code GRANTOR = '_SYSTEM'} rows (Mimer SQL's synthetic "the creator implicitly holds
     * EXECUTE WITH GRANT OPTION" grant - per the docs, a program's creator is automatically
     * granted this at creation time) are excluded - {@code EXT_OBJECT_PRIVILEGES} carries the
     * same kind of synthetic row for a databank's owner too (see {@code
     * MimerDatabankPrivilege.PrivilegeCache}). When the creator is known, any further grant to
     * the creator is redundant too and is filtered the same way.
     */
    static class PrivilegeCache extends JDBCObjectCache<MimerProgram, MimerProgramPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProgram program) throws SQLException {
            String creator = CommonUtils.trim(program.getCreator());
            boolean haveCreator = !CommonUtils.isEmpty(creator);
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = 'IDENT' AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'EXECUTE'\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                (haveCreator ? "  AND GRANTEE <> ?\n" : "") +
                "ORDER BY GRANTEE");
            stmt.setString(1, program.getName());
            if (haveCreator) {
                stmt.setString(2, creator);
            }
            return stmt;
        }

        @Override
        protected MimerProgramPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerProgramPrivilege(program, resultSet);
        }
    }

    static class TablePrivilegeCache extends JDBCObjectCache<MimerProgram, MimerIdentTablePrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProgram program) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, program.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentTablePrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentTablePrivilege(program, resultSet);
        }
    }

    static class ColumnPrivilegeCache extends JDBCObjectCache<MimerProgram, MimerIdentColumnPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProgram program) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.COLUMN_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, program.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentColumnPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentColumnPrivilege(program, resultSet);
        }
    }

    static class ObjectPrivilegeCache extends JDBCObjectCache<MimerProgram, MimerIdentObjectPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProgram program) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_SCHEMA, OBJECT_NAME, OBJECT_TYPE, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE GRANTEE = ? AND PRIVILEGE_TYPE IN ('EXECUTE', 'SEQUENCE', 'TABLE', 'USAGE')\n" +
                "ORDER BY OBJECT_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, program.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentObjectPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentObjectPrivilege(program, resultSet);
        }
    }

    static class GroupMembershipCache extends JDBCObjectCache<MimerProgram, MimerIdentGroupMembership> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProgram program) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_NAME, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE GRANTEE = ? AND PRIVILEGE_TYPE = 'MEMBER'\n" +
                "ORDER BY OBJECT_NAME");
            dbStat.setString(1, program.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentGroupMembership fetchObject(@NotNull JDBCSession session, @NotNull MimerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentGroupMembership(program, resultSet);
        }
    }

    static class SystemPrivilegeCache extends JDBCObjectCache<MimerProgram, MimerSystemPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProgram program) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_SYSTEM_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY PRIVILEGE_TYPE");
            dbStat.setString(1, program.getName());
            return dbStat;
        }

        @Override
        protected MimerSystemPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSystemPrivilege(program, resultSet);
        }
    }
}
