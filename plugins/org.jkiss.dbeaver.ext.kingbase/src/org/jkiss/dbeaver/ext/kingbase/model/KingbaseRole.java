/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseRole
 */
public class KingbaseRole implements
    KingbaseObject,
    KingbasePrivilegeOwner,
    DBPPersistedObject,
    DBPSaveableObject,
    DBPRefreshableObject,
    DBPNamedObject2,
    DBARole,
    DBAUser,
    KingbaseScriptObject,
    DBPScriptObjectExt2
{

    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_FLAGS = "Flags";

    private static final Log log = Log.getLog(KingbaseRole.class);

    protected final KingbaseDatabase database;
    protected long oid;
    protected String name;
    protected boolean superUser;
    protected boolean inherit;
    protected boolean createRole;
    protected boolean createDatabase;
    protected boolean canLogin;
    protected boolean replication;
    protected boolean bypassRls;
    protected int connLimit;
    protected String password;
    protected String validUntil;
    protected String description;
    protected boolean persisted;
    private final MembersCache membersCache = new MembersCache(true);
    private final MembersCache belongsCache = new MembersCache(false);
    private List<KingbaseRoleSetting> extraSettings;

    private final String lineBreak = System.lineSeparator();

    static class MembersCache extends JDBCObjectCache<KingbaseRole, KingbaseRoleMember> {
        private final boolean members;
        MembersCache(boolean members) {
            this.members = members;
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseRole owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM sys_catalog.sys_auth_members WHERE " + (members ? "roleid" : "member")+ "=?");
            dbStat.setLong(1, owner.getObjectId());
            return dbStat;
        }

        @Override
        protected KingbaseRoleMember fetchObject(@NotNull JDBCSession session, @NotNull KingbaseRole owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new KingbaseRoleMember(owner, dbResult);
        }

    }

    public KingbaseRole(KingbaseDatabase database, String name, String password, boolean isUser) {
        this.database = database;
        this.name = name;
        this.password = password;
        this.canLogin = isUser;
        this.persisted = false;
    }

    public KingbaseRole(KingbaseDatabase database, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.loadInfo(dbResult);
    }

    protected void loadInfo(ResultSet dbResult) {
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
        this.validUntil = JDBCUtils.safeGetString(dbResult, "rolvaliduntil");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
    }

    @Nullable
    @Override
    @Property(viewable = true,
        editable = true,
        updatable = true,
        length = PropertyLength.MULTILINE, order = 50,
        visibleIf = CommentsOnRolesSupportedValidator.class)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public KingbaseDataSource getDataSource() {
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
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @NotNull
    @Override
    public KingbaseDatabase getDatabase() {
        return database;
    }

    @Property(viewable = true, order = 3)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(editable = true, updatable = true, order = 10, visibleIf = KingbaseRoleCanBeSuperUserValidator.class)
    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }

    @Property(editable = true, updatable = true, order = 11, visibleIf = KingbaseRoleInheritValidator.class)
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

    @Property(editable = true, updatable = true, order = 13, visibleIf = KingbaseRoleCanCreateDBValidator.class)
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

    @Property(editable = true, updatable = true, order = 15, visibleIf = RoleCanBeReplicationValidator.class)
    public boolean isReplication() {
        return replication;
    }

    public void setReplication(boolean replication) {
        this.replication = replication;
    }

    @Property(editable = true, updatable = true, order = 16, visibleIf = RoleCanBypassRLSValidator.class)
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

    @Property(viewable = true, password = true, editable = true, order = 2, visibleIf = PersistenceUserValidator.class)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Property(category = CAT_SETTINGS, editable = true, updatable = true, order = 22)
    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }

    @Association
    public Collection<KingbaseRoleMember> getMembers(DBRProgressMonitor monitor) throws DBException {
        return membersCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<KingbaseRoleMember> getBelongs(DBRProgressMonitor monitor) throws DBException {
        return belongsCache.getAllObjects(monitor, this);
    }

    @Override
    public KingbaseSchema getSchema() {
        return null;
    }

    @Override
    public KingbaseRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return this;
    }

    private void loadExtraConfigParameters(@NotNull DBRProgressMonitor monitor) throws DBCException {
        extraSettings = new ArrayList<>();
       

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load configuration parameters")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                """
                    select s.setconfig, pd.datname from sys_catalog.sys_db_role_setting s
                    left join sys_catalog.sys_database pd on s.setdatabase = pd.oid
                    where s.setrole = ?""")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String[] setconfig = KingbaseUtils.safeGetStringArray(dbResult, "setconfig");
                        if (ArrayUtils.isEmpty(setconfig)) {
                            continue;
                        }
                        String databaseName = JDBCUtils.safeGetString(dbResult, "datname");
                        KingbaseDatabase database = null;
                        if (CommonUtils.isNotEmpty(databaseName)) {
                            database = getDataSource().getDatabase(databaseName);
                        }
                        for (String parameter : setconfig) {
                            extraSettings.add(new KingbaseRoleSetting(database, parameter));
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Can't read extra role configuration parameters.");
            }
        }
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return DBPScriptObject.OPTION_INCLUDE_PERMISSIONS.equals(option);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        final String lineBreak = System.lineSeparator();
        KingbaseDataSource dataSource = getDataSource();
        final KingbaseServerExtension extension = dataSource.getServerType();
        StringBuilder ddl = new StringBuilder();
        String roleName = DBUtils.getQuotedIdentifier(this);
        ddl.append("-- DROP ROLE ").append(roleName).append(";\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
        ddl.append("CREATE ROLE ").append(roleName).append(" WITH ");
        if (extension.supportsSuperusers()) {
            addOptionToDDL(ddl, isSuperUser(), "SUPERUSER");
        }
        if (extension.supportsRolesWithCreateDBAbility()) {
            addOptionToDDL(ddl, isCreateDatabase(), "CREATEDB");
        }
        addOptionToDDL(ddl, isCreateRole(), "CREATEROLE");
        if (extension.supportsInheritance()) {
            addOptionToDDL(ddl, isInherit(), "INHERIT");
        }
        addOptionToDDL(ddl, isCanLogin(), "LOGIN");
        if (extension.supportsRoleReplication()) {
            addOptionToDDL(ddl, isReplication(), "REPLICATION");
        }
        if (extension.supportsRoleBypassRLS()) {
            addOptionToDDL(ddl, isBypassRls(), "BYPASSRLS");
        }
        if (getConnLimit() > 0) {
            ddl.append(lineBreak);
            ddl.append("\tCONNECTION LIMIT ").append(getConnLimit());
        } else {
            ddl.append(lineBreak);
            ddl.append("\tCONNECTION LIMIT -1");
        }
        if (getValidUntil() != null) {
            ddl.append(lineBreak);
            ddl.append("\tVALID UNTIL '").append(getValidUntil()).append("'");
        }
        ddl.append(";");

        if (extraSettings == null) {
            loadExtraConfigParameters(monitor);
        }
        if (!CommonUtils.isEmpty(extraSettings)) {
            String beginning = "\nALTER ROLE " + roleName + " ";
            for (KingbaseRoleSetting setting : extraSettings) {
                ddl.append(beginning);
                if (setting.database != null) {
                    ddl.append("IN DATABASE ").append(DBUtils.getQuotedIdentifier(setting.database)).append(" ");
                }
                ddl.append("SET ").append(setting.configurationParameter).append(";");
            }
        }
        if (CommonUtils.isNotEmpty(description)) {
            ddl.append("\n\n")
                .append("COMMENT ON ROLE ")
                .append(roleName)
                .append(" IS ")
                .append(SQLUtils.quoteString(this, description))
                .append(";");
        }
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
            ddl.append("\n");
            List<DBEPersistAction> actions = new ArrayList<>();
            KingbaseUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
            ddl.append("\n").append(SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false));
        }
        if (isInherit()) {
            ddl.append("\n");
            for (KingbaseRoleMember member : belongsCache.getAllObjects(monitor, this)) {
                ddl.append("\n")
                    .append("GRANT ")
                    .append(DBUtils.getQuotedIdentifier(member.getOwner(monitor)))
                    .append(" TO ")
                    .append(DBUtils.getQuotedIdentifier(this))
                    .append(";");
            }
        }

        return ddl.toString();
    }

    private void addOptionToDDL(StringBuilder ddl, boolean isOptionOn, String option) {
        ddl.append(lineBreak).append("\t");
        if (isOptionOn) {
            ddl.append(option);
        } else {
            ddl.append("NO").append(option);
        }
    }

    @Override
    public List<KingbasePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBCException {
        List<KingbasePrivilege> permissions = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read role privileges")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM information_schema.table_privileges WHERE table_catalog=? AND grantee=?")) {
                dbStat.setString(1, getDatabase().getName());
                dbStat.setString(2, getName());
                permissions.addAll(getRolePermissions(monitor, this, KingbasePrivilegeGrant.Kind.TABLE, dbStat));
            } catch (Throwable e) {
                log.error("Error reading table privileges", e);
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM information_schema.routine_privileges WHERE specific_catalog=? AND grantee=?")) {
                dbStat.setString(1, getDatabase().getName());
                dbStat.setString(2, getName());
                permissions.addAll(getRolePermissions(monitor, this, KingbasePrivilegeGrant.Kind.FUNCTION, dbStat));
            } catch (Throwable e) {
                log.error("Error reading routine privileges", e);
            }
            // Select acl for all schemas, sequences and materialized views
            boolean supportsDistinct = getDataSource().getServerType().supportsDistinctForStatementsWithAcl(); // Greenplum do not support DISTINCT keyword with the acl data type in the query
            boolean supportsOnlySchemasPermissions = false; 
            String otherObjectsSQL;
            if (supportsOnlySchemasPermissions) {
                otherObjectsSQL = "SELECT n.oid, n.nspacl FROM sys_catalog.sys_namespace n WHERE n.nspacl IS NOT NULL";
            } else {
                otherObjectsSQL = "SELECT * FROM (\n" +
                    "\tSELECT " + (supportsDistinct ? "DISTINCT" : "") + " relnamespace,\n" +
                    "\trelacl,\n" +
                    "\trelname,\n" +
                    "\trelkind,\n" +
                    "(aclexplode(relacl)).grantee as granteeI\n" +
                    "FROM\n" +
                    "\tsys_class\n" +
                    "WHERE\n" +
                    "\trelacl IS NOT NULL\n" +
                    "\tAND relnamespace IN (\n" +
                    "SELECT oid\n" +
                    "FROM sys_namespace\n" +
                    "WHERE nspname NOT LIKE 'sys_%' AND nspname != 'information_schema')\n" +
                    "UNION ALL\n" +
                    "SELECT " + (supportsDistinct ? "DISTINCT" : "") +
                    "\n\tn.oid AS relnamespace,\n" +
                    "\tnspacl AS relacl,\n" +
                    "\tn.nspname AS relname,\n" +
                    "\tcast('C' as \"char\") AS relkind,\n" +
                    "(aclexplode(nspacl)).grantee as granteeI\n" +
                    "FROM\n" +
                    "\tsys_catalog.sys_namespace n\n" +
                    "WHERE\n" +
                    "\tn.nspacl IS NOT NULL \n" +
                    "\t) AS tr\n" +
                    "WHERE tr.granteeI=?" +
                    " AND tr.relkind IN('S', 'm', 'C')";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(otherObjectsSQL)) {
                if (!supportsOnlySchemasPermissions) {
                    dbStat.setLong(1, getObjectId());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        long schemaId = JDBCUtils.safeGetLong(dbResult, 1);
                        Object acl = JDBCUtils.safeGetObject(dbResult, 2);
                        String objectName = null;
                        String objectType = null;
                        if (!supportsOnlySchemasPermissions) {
                            objectName = JDBCUtils.safeGetString(dbResult, "relname");
                            objectType = JDBCUtils.safeGetString(dbResult, "relkind");
                        }
                        KingbaseSchema schema = getDatabase().getSchema(monitor, schemaId);
                        if (schema != null) {
                            List<KingbasePrivilege> privileges = null;
                            KingbasePrivilegeGrant.Kind pKind = null;
                            if (supportsOnlySchemasPermissions) {
                                pKind = KingbasePrivilegeGrant.Kind.SCHEMA;
                                privileges = KingbaseUtils.extractPermissionsFromACL(monitor, schema, acl, false);
                            } else if (objectType != null && objectName != null) {
                                pKind = KingbasePrivilegeGrant.Kind.TABLE;
                                if (objectType.equals("C")) {
                                    privileges = KingbaseUtils.extractPermissionsFromACL(monitor, schema, acl, false);
                                    pKind = KingbasePrivilegeGrant.Kind.SCHEMA;
                                } else if (KingbaseClass.RelKind.S.getCode().equals(objectType)) {
                                    KingbaseSequence sequence = schema.getSequence(monitor, objectName);
                                    privileges = KingbaseUtils.extractPermissionsFromACL(monitor, sequence, acl, false);
                                    pKind = KingbasePrivilegeGrant.Kind.SEQUENCE;
                                } else {
                                    KingbaseMaterializedView materializedView = schema.getMaterializedView(monitor, objectName);
                                    privileges = KingbaseUtils.extractPermissionsFromACL(monitor, materializedView, acl, false);
                                }
                            }
                            for (KingbasePrivilege p : CommonUtils.safeCollection(privileges)) {
                                if (p instanceof KingbaseObjectPrivilege) {
                                    KingbaseRoleReference grantee = ((KingbaseObjectPrivilege) p).getGrantee();
                                    if (grantee != null && this.isReferencedWith(grantee)) {
                                        List<KingbasePrivilegeGrant> grants = new ArrayList<>();
                                        for (KingbasePrivilege.ObjectPermission perm : p.getPermissions()) {
                                            grants.add(new KingbasePrivilegeGrant(
                                                perm.getGrantor(),
                                                grantee,
                                                getDatabase().getName(),
                                                schema.getName(),
                                                objectName, perm.getPrivilegeType(),
                                                false,
                                                false
                                            ));
                                        }
                                        permissions.add(
                                            new KingbaseRolePrivilege(
                                                this,
                                                pKind,
                                                schema.getName(),
                                                objectName,
                                                grants));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (getDataSource().getServerType().supportsDefaultPrivileges()) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    """
                        SELECT DISTINCT g.* FROM (
                        SELECT *,
                        (aclexplode(defaclacl)).grantee as grantee
                        FROM sys_default_acl a WHERE a.defaclnamespace <> 0) as g
                        where g.grantee = ?""")) {
                    dbStat.setLong(1, getObjectId());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.nextRow()) {
                            long schemaId = JDBCUtils.safeGetLong(dbResult, "defaclnamespace");
                            KingbaseSchema schema = getDatabase().getSchema(monitor, schemaId);
                            if (schema == null) {
                                continue;
                            }
                            Object acl = JDBCUtils.safeGetObject(dbResult, "defaclacl");
                            if (acl == null) {
                                continue;
                            }
                            String objectType = JDBCUtils.safeGetString(dbResult, "defaclobjtype");
                            if (CommonUtils.isEmpty(objectType)) {
                                log.debug("Can't read default permissions object type for " + schema.getName());
                                continue;
                            }
                            List<KingbasePrivilege> privileges = KingbaseUtils.extractPermissionsFromACL(monitor, schema, acl, true);
                            List<KingbasePrivilege> resultPrivileges = new ArrayList<>();
                            for (KingbasePrivilege privilege : privileges) {
                                if (privilege instanceof KingbaseDefaultPrivilege defaultPrivilege) {
                                    if (!defaultPrivilege.getGrantee().equals(getName())) {
                                        continue;
                                    }
                                    defaultPrivilege.setUnderKind(objectType);
                                    resultPrivileges.add(defaultPrivilege);
                                }
                            }
                            permissions.addAll(resultPrivileges);
                            schema.addDefaultPrivileges(resultPrivileges);
                        }
                    }
                } catch (Throwable e) {
                    log.error("Error reading default privileges", e);
                }
            }
            Collections.sort(permissions);
        } catch (Exception e) {
            throw new DBCException("Error reading role privileges", e);
        }
        return permissions;
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return null;
    }

    protected static Collection<KingbasePrivilege> getRolePermissions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull KingbaseRole role,
        @NotNull KingbasePrivilegeGrant.Kind kind,
        @NotNull JDBCPreparedStatement dbStat) throws SQLException
    {
        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
            Map<String, List<KingbasePrivilegeGrant>> privs = new LinkedHashMap<>();
            while (dbResult.next()) {
                KingbasePrivilegeGrant privilege = new KingbasePrivilegeGrant(role.database, kind, dbResult);
                String privilegeObjectName = privilege.getObjectName();
                String objectSchema = privilege.getObjectSchema();
                if ((kind == KingbasePrivilegeGrant.Kind.FUNCTION || kind == KingbasePrivilegeGrant.Kind.PROCEDURE)
                    && CommonUtils.isNotEmpty(privilegeObjectName) && privilegeObjectName.contains("_")
                    && !privilegeObjectName.endsWith("_") && CommonUtils.isNotEmpty(objectSchema))
                {
                    changeRoutineFullName(monitor, role, privilege, privilegeObjectName, objectSchema);
                }
                String tableId = objectSchema + "." + privilege.getObjectName();
                List<KingbasePrivilegeGrant> privList = privs.computeIfAbsent(tableId, k -> new ArrayList<>());
                privList.add(privilege);
            }
            // Pack to permission list
            List<KingbasePrivilege> result = new ArrayList<>(privs.size());
            for (List<KingbasePrivilegeGrant> priv : privs.values()) {
                KingbasePrivilegeGrant privilegeGrant = priv.get(0);
                result.add(new KingbaseRolePrivilege(
                    role,
                    privilegeGrant.getKind(),
                    privilegeGrant.getObjectSchema(),
                    privilegeGrant.getObjectName(),
                    priv));
            }
            return result;
        }
    }

    private static void changeRoutineFullName(
        @NotNull DBRProgressMonitor monitor,
        @NotNull KingbaseRole role,
        @NotNull KingbasePrivilegeGrant privilege,
        String privilegeObjectName,
        String objectSchema)
    {
        String privId = privilegeObjectName.substring(privilegeObjectName.lastIndexOf("_") + 1);
        long routineId = CommonUtils.toLong(privId, -1);
        if (routineId != -1) {
            KingbaseDatabase database = role.getDatabase();
            KingbaseSchema schema;
            try {
                schema = database.getSchema(monitor, objectSchema);
            } catch (DBException e) {
                log.debug("Can't find routine schema '" + objectSchema + "'", e);
                schema = database.getPublicSchema();
            }
            if (schema != null) {
                KingbaseProcedure procedure = null;
                try {
                    procedure = schema.getProcedure(monitor, routineId);
                } catch (DBException e) {
                    log.debug("Can't find routine in schema '" + privilegeObjectName + "'", e);
                }
                if (procedure != null && CommonUtils.isNotEmpty(procedure.getOverloadedName())) {
                    privilege.setObjectName(procedure.getOverloadedName());
                    if (procedure.getKind() == KingbaseProcedureKind.p) {
                        // They all are FUNCTIONS by default
                        privilege.setKind(KingbasePrivilegeGrant.Kind.PROCEDURE);
                    }
                }
            }
        }
    }

    
    @Nullable
    public String getSpecificRoleType() {
        return null;
    }

    public KingbaseRoleReference getRoleReference() {
        return new KingbaseRoleReference(this.database, this.getName(), this.getSpecificRoleType());
    }

    public boolean isReferencedWith(KingbaseRoleReference reference) {
        return reference != null
            && Objects.equals(this.getDatabase(), reference.getDatabase())
            && Objects.equals(this.getName(), reference.getRoleName())
            && Objects.equals(this.getSpecificRoleType(), reference.getRoleType());
    }
    
    public boolean supportsRoutinesPermissions() {
        return true;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) {
        membersCache.clearCache();
        belongsCache.clearCache();
        extraSettings = null;
        return this;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static class KingbaseRoleCanBeSuperUserValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsSuperusers();
        }
    }

    public static class KingbaseRoleInheritValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsInheritance();
        }
    }

    public static class KingbaseRoleCanCreateDBValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRolesWithCreateDBAbility();
        }
    }

    public static class RoleCanBeReplicationValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRoleReplication();
        }
    }

    public static class RoleCanBypassRLSValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRoleBypassRLS();
        }
    }

    public static class PersistenceUserValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return !object.isPersisted();
        }
    }

    public static class CommentsOnRolesSupportedValidator implements IPropertyValueValidator<KingbaseRole, Object> {
        @Override
        public boolean isValidValue(KingbaseRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsCommentsOnRole();
        }
    }

    private static class KingbaseRoleSetting {

        @Nullable KingbaseDatabase database;
        @NotNull String configurationParameter;

        KingbaseRoleSetting(@Nullable KingbaseDatabase database, @NotNull String configurationParameter) {
            this.database = database;
            this.configurationParameter = configurationParameter;
        }
    }
}
