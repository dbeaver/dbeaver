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
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Mimer SQL USER/OS_USER ident, read from {@code INFORMATION_SCHEMA.EXT_IDENTS} ({@code
 * IDENT_TYPE IN ('USER', 'OS_USER')}, see {@link MimerDataSource.UserCache}). Carries the {@code
 * CREATE IDENT} password/schema-clause attributes for {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerUserManager}, and lets the properties view change an
 * existing user's password ({@code ALTER IDENT SET PASSWORD}). OS login authorizations are a
 * separate, multi-row "Authorizations" folder - see {@link MimerUserAuthorization} - not a
 * single property here; a user can have several ({@code ALTER IDENT ADD OS_USER} once per
 * authorization). {@code EXT_IDENTS} has one row per {@code (IDENT_NAME, IDENT_LOGIN)} pair,
 * not one row per ident with a single login column.
 *
 * @author Mimer Information Technology
 */
public class MimerUser implements DBSObject, DBPNamedObject2, DBPSaveableObject, DBPRefreshableObject {

    public static final String TYPE_USER = "USER";
    public static final String TYPE_OS_USER = "OS_USER";

    private final AuthorizationCache authorizationCache = new AuthorizationCache();
    private final TablePrivilegeCache tablePrivilegeCache = new TablePrivilegeCache();
    private final ColumnPrivilegeCache columnPrivilegeCache = new ColumnPrivilegeCache();
    private final ObjectPrivilegeCache objectPrivilegeCache = new ObjectPrivilegeCache();
    private final GroupMembershipCache groupMembershipCache = new GroupMembershipCache();
    private final SystemPrivilegeCache systemPrivilegeCache = new SystemPrivilegeCache();
    private final MimerDataSource dataSource;
    private String name;
    private String type;
    private String creator;
    private boolean hasSchema;
    private boolean persisted;

    // CREATE IDENT-only; also reused post-creation to drive ALTER IDENT SET PASSWORD. Always
    // blank on load - EXT_IDENTS only says whether a password is set, never its value.
    private String password;
    private boolean withoutSchema;

    // CREATE-only: groups the new user is granted MEMBER of in the same step. Not re-hydrated -
    // membership lives on MimerGroup.getMembers(), not on the user.
    private List<String> initialGroups = Collections.emptyList();

    private String comment;

    public MimerUser(@NotNull MimerDataSource dataSource, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "IDENT_NAME");
        this.type = JDBCUtils.safeGetStringTrimmed(dbResult, "IDENT_TYPE");
        this.creator = JDBCUtils.safeGetString(dbResult, "IDENT_CREATOR");
        this.hasSchema = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IDENT_SCHEMA"));
        this.persisted = true;
    }

    public MimerUser(@NotNull MimerDataSource dataSource, @NotNull String name) {
        this.dataSource = dataSource;
        this.name = name;
        this.type = TYPE_USER;
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
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 3)
    public String getCreator() {
        return creator;
    }

    @Property(viewable = true, order = 4)
    public boolean isHasSchema() {
        return hasSchema;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 5, password = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isWithoutSchema() {
        return withoutSchema;
    }

    public void setWithoutSchema(boolean withoutSchema) {
        this.withoutSchema = withoutSchema;
    }

    @NotNull
    public List<String> getInitialGroups() {
        return initialGroups;
    }

    public void setInitialGroups(@NotNull List<String> initialGroups) {
        this.initialGroups = initialGroups;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON IDENT "name" IS '...'} - Users, Groups, and Programs are all {@code
     * IDENT}s in Mimer SQL's comment statement and in {@code EXT_OBJECT_IDENT_USAGE.OBJECT_TYPE},
     * not each kind's own DDL keyword. See {@link org.jkiss.dbeaver.ext.mimer.edit.MimerUserManager}
     * for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 7)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, null, null, name, "IDENT");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
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
     * {@code CREATE IDENT "name" AS USER [USING 'password'] [WITHOUT SCHEMA]}. WITH SCHEMA
     * is the default (a schema named after the user is auto-created) - the clause is only
     * ever emitted to opt out via WITHOUT SCHEMA, never to spell out the default.
     */
    @NotNull
    public String buildCreateDDL() {
        StringBuilder sb = new StringBuilder("CREATE IDENT \"").append(name).append("\" AS USER");
        if (!CommonUtils.isEmpty(password)) {
            sb.append(" USING '").append(password.replace("'", "''")).append('\'');
        }
        if (withoutSchema) {
            sb.append(" WITHOUT SCHEMA");
        }
        return sb.toString();
    }

    @NotNull
    public String buildDropDDL() {
        return "DROP IDENT \"" + name + "\"";
    }

    /**
     * One {@code GRANT MEMBER ON GROUP "g" TO "n"} per group in {@link #getInitialGroups()}, run
     * right after {@link #buildCreateDDL()}. No {@code WITH GRANT OPTION} - use the group's own
     * Members folder for that.
     */
    @NotNull
    public List<String> buildGroupGrantDDL() {
        List<String> statements = new ArrayList<>(initialGroups.size());
        for (String group : initialGroups) {
            statements.add("GRANT MEMBER ON GROUP \"" + group + "\" TO \"" + name + "\"");
        }
        return statements;
    }

    /**
     * {@code ALTER IDENT} statement for a changed password. OS login authorizations are handled
     * separately - see {@link MimerUserAuthorization}.
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
     * OS login authorizations for this user - see {@link MimerUserAuthorization}. A user can have
     * several.
     */
    @Association
    public Collection<MimerUserAuthorization> getAuthorizations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return authorizationCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerUser, MimerUserAuthorization> getAuthorizationCache() {
        return authorizationCache;
    }

    /**
     * Schemas owned by this user - {@code INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_OWNER = ?} -
     * resolved back to the real {@link MimerSchema} objects already in the datasource's own schema list rather
     * than a separate read-only leaf class, so this list can be navigated into directly.
     */
    @Association
    public List<MimerSchema> getOwnedSchemas(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<MimerSchema> result = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read Mimer SQL schemas owned by user")) {
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
     * Table/view privileges granted to this user, across every table - see {@link
     * MimerIdentTablePrivilege}.
     */
    @Association
    public Collection<MimerIdentTablePrivilege> getTablePrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tablePrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Column-restricted privileges granted to this user - see {@link MimerIdentColumnPrivilege}.
     */
    @Association
    public Collection<MimerIdentColumnPrivilege> getColumnPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return columnPrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Non-table object privileges (EXECUTE/SEQUENCE/TABLE/USAGE) granted to this user, across
     * every object - see {@link MimerIdentObjectPrivilege}.
     */
    @Association
    public Collection<MimerIdentObjectPrivilege> getObjectPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objectPrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Groups this user is a member of - the reverse direction of {@code MimerGroup#getMembers}.
     */
    @Association
    public Collection<MimerIdentGroupMembership> getGroupMemberships(@NotNull DBRProgressMonitor monitor) throws DBException {
        return groupMembershipCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerUser, MimerIdentGroupMembership> getGroupMembershipCache() {
        return groupMembershipCache;
    }

    /**
     * System-level (not per-object) privileges granted to this user - see {@link
     * MimerSystemPrivilege}.
     */
    @Association
    public Collection<MimerSystemPrivilege> getSystemPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return systemPrivilegeCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerUser, MimerSystemPrivilege> getSystemPrivilegeCache() {
        return systemPrivilegeCache;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) {
        authorizationCache.clearCache();
        tablePrivilegeCache.clearCache();
        columnPrivilegeCache.clearCache();
        objectPrivilegeCache.clearCache();
        groupMembershipCache.clearCache();
        systemPrivilegeCache.clearCache();
        comment = null;
        return this;
    }

    /**
     * One row per {@code (IDENT_NAME, IDENT_LOGIN)} pair - {@code EXT_IDENTS} has a separate row
     * per authorization, not a single login column per ident.
     */
    static class AuthorizationCache extends JDBCObjectCache<MimerUser, MimerUserAuthorization> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUser user) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT IDENT_LOGIN\n" +
                "FROM INFORMATION_SCHEMA.EXT_IDENTS\n" +
                "WHERE IDENT_NAME = ? AND IDENT_LOGIN IS NOT NULL\n" +
                "ORDER BY IDENT_LOGIN");
            dbStat.setString(1, user.getName());
            return dbStat;
        }

        @Override
        protected MimerUserAuthorization fetchObject(@NotNull JDBCSession session, @NotNull MimerUser user, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerUserAuthorization(user, resultSet);
        }
    }

    static class TablePrivilegeCache extends JDBCObjectCache<MimerUser, MimerIdentTablePrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUser user) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, user.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentTablePrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerUser user, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentTablePrivilege(user, resultSet);
        }
    }

    static class ColumnPrivilegeCache extends JDBCObjectCache<MimerUser, MimerIdentColumnPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUser user) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.COLUMN_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, user.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentColumnPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerUser user, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentColumnPrivilege(user, resultSet);
        }
    }

    /**
     * {@code PRIVILEGE_TYPE IN ('EXECUTE','SEQUENCE','TABLE','USAGE')} - matches {@code
     * mimer.getIdentObjectPrivileges} exactly (see {@link MimerIdentObjectPrivilege}'s Javadoc).
     */
    static class ObjectPrivilegeCache extends JDBCObjectCache<MimerUser, MimerIdentObjectPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUser user) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_SCHEMA, OBJECT_NAME, OBJECT_TYPE, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE GRANTEE = ? AND PRIVILEGE_TYPE IN ('EXECUTE', 'SEQUENCE', 'TABLE', 'USAGE')\n" +
                "ORDER BY OBJECT_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, user.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentObjectPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerUser user, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentObjectPrivilege(user, resultSet);
        }
    }

    /**
     * {@code PRIVILEGE_TYPE = 'MEMBER'} - the groups this user belongs to, same underlying view
     * as {@link MimerGroupMember} (which shows it from the group's own side instead).
     */
    static class GroupMembershipCache extends JDBCObjectCache<MimerUser, MimerIdentGroupMembership> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUser user) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_NAME, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE GRANTEE = ? AND PRIVILEGE_TYPE = 'MEMBER'\n" +
                "ORDER BY OBJECT_NAME");
            dbStat.setString(1, user.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentGroupMembership fetchObject(@NotNull JDBCSession session, @NotNull MimerUser user, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentGroupMembership(user, resultSet);
        }
    }

    static class SystemPrivilegeCache extends JDBCObjectCache<MimerUser, MimerSystemPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUser user) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_SYSTEM_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY PRIVILEGE_TYPE");
            dbStat.setString(1, user.getName());
            return dbStat;
        }

        @Override
        protected MimerSystemPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerUser user, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSystemPrivilege(user, resultSet);
        }
    }
}
