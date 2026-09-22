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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
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

import java.sql.SQLException;
import java.util.Collection;

/**
 * Mimer SQL GROUP ident. Read from {@code INFORMATION_SCHEMA.EXT_IDENTS} (see
 * {@link MimerDataSource.GroupCache}, filtered to {@code IDENT_TYPE = 'GROUP'}, plus the
 * built-in {@code PUBLIC} group unioned in since {@code EXT_IDENTS} doesn't return it).
 * Backs {@code CREATE}/{@code DROP IDENT} for
 * {@link org.jkiss.dbeaver.ext.mimer.edit.MimerGroupManager} and owns a {@link MemberCache}
 * of {@link MimerGroupMember}s (the group's {@code MEMBER} privilege grants).
 *
 * @author Mimer Information Technology
 */
public class MimerGroup implements DBSObject, DBPNamedObject2, DBPSaveableObject, DBPRefreshableObject {

    private final MimerDataSource dataSource;
    private final MemberCache memberCache = new MemberCache();
    private final TablePrivilegeCache tablePrivilegeCache = new TablePrivilegeCache();
    private final ColumnPrivilegeCache columnPrivilegeCache = new ColumnPrivilegeCache();
    private final ObjectPrivilegeCache objectPrivilegeCache = new ObjectPrivilegeCache();
    private final GroupMembershipCache groupMembershipCache = new GroupMembershipCache();
    private final SystemPrivilegeCache systemPrivilegeCache = new SystemPrivilegeCache();
    private String name;
    private String creator;
    private boolean hasSchema;
    private boolean persisted;
    private String comment;

    public MimerGroup(@NotNull MimerDataSource dataSource, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "IDENT_NAME");
        this.creator = JDBCUtils.safeGetString(dbResult, "IDENT_CREATOR");
        this.hasSchema = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IDENT_SCHEMA"));
        this.persisted = true;
    }

    public MimerGroup(@NotNull MimerDataSource dataSource, @NotNull String name) {
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
    public boolean isHasSchema() {
        return hasSchema;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON IDENT "name" IS '...'} - see {@link MimerUser#getComment} for why
     * {@code OBJECT_TYPE = 'IDENT'} is correct here too. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerGroupManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 4)
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

    @Association
    public Collection<MimerGroupMember> getMembers(DBRProgressMonitor monitor) throws DBException {
        return memberCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerGroup, MimerGroupMember> getMemberCache() {
        return memberCache;
    }

    /**
     * Table/view privileges granted to this group, across every table - see {@link
     * MimerIdentTablePrivilege}.
     */
    @Association
    public Collection<MimerIdentTablePrivilege> getTablePrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tablePrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Column-restricted privileges granted to this group - see {@link MimerIdentColumnPrivilege}.
     */
    @Association
    public Collection<MimerIdentColumnPrivilege> getColumnPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return columnPrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Non-table object privileges (EXECUTE/SEQUENCE/TABLE/USAGE) granted to this group - see
     * {@link MimerIdentObjectPrivilege}.
     */
    @Association
    public Collection<MimerIdentObjectPrivilege> getObjectPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objectPrivilegeCache.getAllObjects(monitor, this);
    }

    /**
     * Groups this group is itself a member of (nested group membership).
     */
    @Association
    public Collection<MimerIdentGroupMembership> getGroupMemberships(@NotNull DBRProgressMonitor monitor) throws DBException {
        return groupMembershipCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerGroup, MimerIdentGroupMembership> getGroupMembershipCache() {
        return groupMembershipCache;
    }

    /**
     * System-level (not per-object) privileges granted to this group - see {@link
     * MimerSystemPrivilege}.
     */
    @Association
    public Collection<MimerSystemPrivilege> getSystemPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return systemPrivilegeCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerGroup, MimerSystemPrivilege> getSystemPrivilegeCache() {
        return systemPrivilegeCache;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        memberCache.clearCache();
        tablePrivilegeCache.clearCache();
        columnPrivilegeCache.clearCache();
        objectPrivilegeCache.clearCache();
        groupMembershipCache.clearCache();
        systemPrivilegeCache.clearCache();
        comment = null;
        return this;
    }

    @NotNull
    public String buildCreateDDL() {
        return "CREATE IDENT \"" + name + "\" AS GROUP";
    }

    @NotNull
    public String buildDropDDL() {
        return "DROP IDENT \"" + name + "\"";
    }

    static class MemberCache extends JDBCObjectCache<MimerGroup, MimerGroupMember> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerGroup owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = 'IDENT' AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'MEMBER'\n" +
                "ORDER BY GRANTEE");
            stmt.setString(1, owner.getName());
            return stmt;
        }

        @Override
        protected MimerGroupMember fetchObject(@NotNull JDBCSession session, @NotNull MimerGroup owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerGroupMember(owner, resultSet);
        }
    }

    static class TablePrivilegeCache extends JDBCObjectCache<MimerGroup, MimerIdentTablePrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerGroup owner) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentTablePrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerGroup owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentTablePrivilege(owner, resultSet);
        }
    }

    static class ColumnPrivilegeCache extends JDBCObjectCache<MimerGroup, MimerIdentColumnPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerGroup owner) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.COLUMN_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentColumnPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerGroup owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentColumnPrivilege(owner, resultSet);
        }
    }

    static class ObjectPrivilegeCache extends JDBCObjectCache<MimerGroup, MimerIdentObjectPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerGroup owner) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_SCHEMA, OBJECT_NAME, OBJECT_TYPE, PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE GRANTEE = ? AND PRIVILEGE_TYPE IN ('EXECUTE', 'SEQUENCE', 'TABLE', 'USAGE')\n" +
                "ORDER BY OBJECT_NAME, PRIVILEGE_TYPE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentObjectPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerGroup owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentObjectPrivilege(owner, resultSet);
        }
    }

    static class GroupMembershipCache extends JDBCObjectCache<MimerGroup, MimerIdentGroupMembership> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerGroup owner) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_NAME, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE GRANTEE = ? AND PRIVILEGE_TYPE = 'MEMBER'\n" +
                "ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected MimerIdentGroupMembership fetchObject(@NotNull JDBCSession session, @NotNull MimerGroup owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerIdentGroupMembership(owner, resultSet);
        }
    }

    static class SystemPrivilegeCache extends JDBCObjectCache<MimerGroup, MimerSystemPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerGroup owner) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT PRIVILEGE_TYPE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_SYSTEM_PRIVILEGES\n" +
                "WHERE GRANTEE = ?\n" +
                "ORDER BY PRIVILEGE_TYPE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected MimerSystemPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerGroup owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSystemPrivilege(owner, resultSet);
        }
    }
}
