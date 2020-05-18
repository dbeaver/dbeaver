/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * PostgreRole
 */
public class PostgreRole implements PostgreObject, PostgrePrivilegeOwner, DBPPersistedObject, DBPSaveableObject, DBPRefreshableObject, DBPNamedObject2, DBARole, DBAUser {

    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_FLAGS = "Flags";

    private static final Log log = Log.getLog(PostgreRole.class);

    private final PostgreDatabase database;
    private long oid;
    private String name;
    private boolean superUser;
    private boolean inherit;
    private boolean createRole;
    private boolean createDatabase;
    private boolean canLogin;
    private boolean replication;
    private boolean bypassRls;
    private int connLimit;
    private String password;
    private Date validUntil;
    private boolean persisted;
    private MembersCache membersCache = new MembersCache(true);
    private MembersCache belongsCache = new MembersCache(false);

    static class MembersCache extends JDBCObjectCache<PostgreRole, PostgreRoleMember> {
        private final boolean members;
        MembersCache(boolean members) {
            this.members = members;
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreRole owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM pg_catalog.pg_auth_members WHERE " + (members ? "roleid" : "member")+ "=?");
            dbStat.setLong(1, owner.getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreRoleMember fetchObject(@NotNull JDBCSession session, @NotNull PostgreRole owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreRoleMember(owner, dbResult);
        }

    }

    public PostgreRole(PostgreDatabase database, String name, String password, boolean isUser) {
        this.database = database;
        this.name = name;
        this.password = password;
        this.canLogin = isUser;
        this.persisted = false;
    }

    public PostgreRole(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult) {
        this.persisted = true;

        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "rolname");
        this.superUser = JDBCUtils.safeGetBoolean(dbResult, "rolsuper");
        this.inherit = JDBCUtils.safeGetBoolean(dbResult, "rolinherit");
        this.createRole = JDBCUtils.safeGetBoolean(dbResult, "rolcreaterole");
        this.createDatabase = JDBCUtils.safeGetBoolean(dbResult, "rolcreatedb");
        this.canLogin = JDBCUtils.safeGetBoolean(dbResult, "rolcanlogin");
        this.replication = JDBCUtils.safeGetBoolean(dbResult, "rolreplication");
        this.bypassRls = JDBCUtils.safeGetBoolean(dbResult, "rolbypassrls");
        this.connLimit = JDBCUtils.safeGetInt(dbResult, "rolconnlimit");
        this.password = JDBCUtils.safeGetString(dbResult, "rolpassword");
        this.validUntil = JDBCUtils.safeGetTimestamp(dbResult, "rolvaliduntil");
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    public boolean isUser() {
        return canLogin;
    }

    @Override
    public boolean isPersisted() {
        return this.persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @Property(viewable = true, order = 2)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(editable = true, updatable = true, order = 10)
    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }

    @Property(editable = true, updatable = true, order = 11)
    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    @Property(editable = true, updatable = true, order = 12)
    public boolean isCreateRole() {
        return createRole;
    }

    public void setCreateRole(boolean createRole) {
        this.createRole = createRole;
    }

    @Property(editable = true, updatable = true, order = 13)
    public boolean isCreateDatabase() {
        return createDatabase;
    }

    public void setCreateDatabase(boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    @Property(editable = true, updatable = true, order = 14)
    public boolean isCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    @Property(editable = true, updatable = true, order = 15)
    public boolean isReplication() {
        return replication;
    }

    public void setReplication(boolean replication) {
        this.replication = replication;
    }

    @Property(editable = true, updatable = true, order = 16)
    public boolean isBypassRls() {
        return bypassRls;
    }

    public void setBypassRls(boolean bypassRls) {
        this.bypassRls = bypassRls;
    }

    @Property(category = CAT_SETTINGS, editable = true, updatable = true, order = 20)
    public int getConnLimit() {
        return connLimit;
    }

    public void setConnLimit(int connLimit) {
        this.connLimit = connLimit;
    }

    @Property(hidden = true, category = CAT_SETTINGS, editable = true, updatable = true, order = 21)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Property(category = CAT_SETTINGS, editable = true, updatable = true, order = 22)
    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    @Association
    public Collection<PostgreRoleMember> getMembers(DBRProgressMonitor monitor) throws DBException {
        return membersCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreRoleMember> getBelongs(DBRProgressMonitor monitor) throws DBException {
        return belongsCache.getAllObjects(monitor, this);
    }

    @Override
    public PostgreSchema getSchema() {
        return null;
    }

    @Override
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        // Dummy implementation
        return this;
    }

    @Override
    public List<PostgrePrivilege> getPrivileges(DBRProgressMonitor monitor, boolean includeNestedObjects) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read role privileges")) {
            List<PostgrePrivilege> permissions = new ArrayList<>();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM information_schema.table_privileges WHERE table_catalog=? AND grantee=?")) {
                dbStat.setString(1, getDatabase().getName());
                dbStat.setString(2, getName());
                permissions.addAll(getRolePermissions(this, PostgrePrivilegeGrant.Kind.TABLE, dbStat));
            } catch (Throwable e) {
                log.error("Error reading table privileges", e);
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM information_schema.routine_privileges WHERE specific_catalog=? AND grantee=?")) {
                dbStat.setString(1, getDatabase().getName());
                dbStat.setString(2, getName());
                permissions.addAll(getRolePermissions(this, PostgrePrivilegeGrant.Kind.FUNCTION, dbStat));
            } catch (Throwable e) {
                log.error("Error reading routine privileges", e);
            }
            // Select acl for all schemas
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT n.oid, n.nspacl FROM pg_catalog.pg_namespace n WHERE n.nspacl IS NOT NULL")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        long schemaId = JDBCUtils.safeGetLong(dbResult, 1);
                        Object acl = JDBCUtils.safeGetObject(dbResult, 2);
                        PostgreSchema schema = getDatabase().getSchema(monitor, schemaId);
                        if (schema != null) {
                            List<PostgrePrivilege> privileges = PostgreUtils.extractPermissionsFromACL(monitor, schema, acl);
                            for (PostgrePrivilege p : privileges) {
                                if (p instanceof PostgreObjectPrivilege && getName().equals(((PostgreObjectPrivilege) p).getGrantee())) {
                                    List<PostgrePrivilegeGrant> grants = new ArrayList<>();
                                    for (PostgrePrivilege.ObjectPermission perm : p.getPermissions()) {
                                        grants.add(new PostgrePrivilegeGrant(perm.getGrantor(), getName(), getDatabase().getName(), schema.getName(), null, perm.getPrivilegeType(), false, false));
                                    }
                                    permissions.add(
                                        new PostgreRolePrivilege(
                                            this,
                                            PostgrePrivilegeGrant.Kind.SCHEMA,
                                            schema.getName(),
                                            null,
                                            grants));
                                }
                            }
                        }
                    }
                }
                //permissions.addAll(getRolePermissions(this, PostgrePrivilegeGrant.Kind.FUNCTION, dbStat));
            } catch (Throwable e) {
                log.error("Error reading routine privileges", e);
            }
            Collections.sort(permissions);
            return permissions;
        }
    }

    @Override
    public String generateChangeOwnerQuery(String owner) {
        return null;
    }

    private static Collection<PostgrePrivilege> getRolePermissions(PostgreRole role, PostgrePrivilegeGrant.Kind kind, JDBCPreparedStatement dbStat) throws SQLException {
        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
            Map<String, List<PostgrePrivilegeGrant>> privs = new LinkedHashMap<>();
            while (dbResult.next()) {
                PostgrePrivilegeGrant privilege = new PostgrePrivilegeGrant(kind, dbResult);
                String tableId = privilege.getObjectSchema() + "." + privilege.getObjectName();
                List<PostgrePrivilegeGrant> privList = privs.computeIfAbsent(tableId, k -> new ArrayList<>());
                privList.add(privilege);
            }
            // Pack to permission list
            List<PostgrePrivilege> result = new ArrayList<>(privs.size());
            for (List<PostgrePrivilegeGrant> priv : privs.values()) {
                result.add(new PostgreRolePrivilege(role, kind, priv.get(0).getObjectSchema(), priv.get(0).getObjectName(), priv));
            }
            return result;
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) {
        membersCache.clearCache();
        belongsCache.clearCache();
        return this;
    }

    @Override
    public String toString() {
        return getName();
    }
}

