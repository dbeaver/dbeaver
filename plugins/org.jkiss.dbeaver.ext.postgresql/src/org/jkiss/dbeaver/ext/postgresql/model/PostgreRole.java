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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PostgreRole
 */
public class PostgreRole implements
    PostgreObject,
    PostgrePrivilegeOwner,
    DBPPersistedObject,
    DBPSaveableObject,
    DBPRefreshableObject,
    DBPNamedObject2,
    DBARole,
    DBAUser,
    PostgreScriptObject,
    DBPScriptObjectExt2
{

    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_FLAGS = "Flags";

    private static final Log log = Log.getLog(PostgreRole.class);

    protected final PostgreDatabase database;
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
    protected LocalDateTime validUntil;
    protected String description;
    protected boolean persisted;
    private final MembersCache membersCache = new MembersCache(true);
    private final MembersCache belongsCache = new MembersCache(false);
    private List<PostgreRoleSetting> extraSettings;

    private final String lineBreak = System.lineSeparator();

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
        this.validUntil = Optional.ofNullable(JDBCUtils.safeGetTimestamp(dbResult, "rolvaliduntil"))
            .map(Timestamp::toLocalDateTime)
            .orElse(null);
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
    public PostgreDatabase getDatabase() {
        return database;
    }

    @Property(viewable = true, order = 3)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(editable = true, updatable = true, order = 10, visibleIf = PostgreRoleCanBeSuperUserValidator.class)
    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }

    @Property(editable = true, updatable = true, order = 11, visibleIf = PostgreRoleInheritValidator.class)
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

    @Property(editable = true, updatable = true, order = 13, visibleIf = PostgreRoleCanCreateDBValidator.class)
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
    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
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

    private void loadExtraConfigParameters(@NotNull DBRProgressMonitor monitor) throws DBCException {
        extraSettings = new ArrayList<>();
        if (!getDataSource().isServerVersionAtLeast(9, 0)) {
            // Not supported
            return;
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load configuration parameters")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                """
                    select s.setconfig, pd.datname from pg_catalog.pg_db_role_setting s
                    left join pg_catalog.pg_database pd on s.setdatabase = pd.oid
                    where s.setrole = ?""")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String[] setconfig = PostgreUtils.safeGetStringArray(dbResult, "setconfig");
                        if (ArrayUtils.isEmpty(setconfig)) {
                            // something went wrong
                            continue;
                        }
                        String databaseName = JDBCUtils.safeGetString(dbResult, "datname");
                        PostgreDatabase database = null;
                        if (CommonUtils.isNotEmpty(databaseName)) {
                            database = getDataSource().getDatabase(databaseName);
                        }
                        for (String parameter : setconfig) {
                            parameter = quoteParamIfNeed(parameter);
                            extraSettings.add(new PostgreRoleSetting(database, parameter));
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Can't read extra role configuration parameters.");
            }
        }
    }

    private String quoteParamIfNeed(String parameter) {

        if (parameter == null || parameter.isEmpty()) {
            return parameter;
        }
        int valueStartingIndex = parameter.indexOf("=");
        if (valueStartingIndex < 0 || valueStartingIndex + 1 >= parameter.length()) {
            return parameter;
        }
        valueStartingIndex = valueStartingIndex + 1;
        String value = parameter.substring(valueStartingIndex);
        if (CommonUtils.isNumber(value) || (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')) {
            return parameter;
        }
        return parameter.substring(0, valueStartingIndex) + SQLUtils.quoteString(this, value);
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
        PostgreDataSource dataSource = getDataSource();
        final PostgreServerExtension extension = dataSource.getServerType();
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
            for (PostgreRoleSetting setting : extraSettings) {
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
            PostgreUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
            ddl.append("\n").append(SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false));
        }
        if (isInherit()) {
            ddl.append("\n");
            for (PostgreRoleMember member : belongsCache.getAllObjects(monitor, this)) {
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
    public List<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBCException {
        List<PostgrePrivilege> permissions = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read role privileges")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM information_schema.table_privileges WHERE table_catalog=? AND grantee=?")) {
                dbStat.setString(1, getDatabase().getName());
                dbStat.setString(2, getName());
                permissions.addAll(getRolePermissions(monitor, this, PostgrePrivilegeGrant.Kind.TABLE, dbStat));
            } catch (Throwable e) {
                log.error("Error reading table privileges", e);
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM information_schema.routine_privileges WHERE specific_catalog=? AND grantee=?")) {
                dbStat.setString(1, getDatabase().getName());
                dbStat.setString(2, getName());
                permissions.addAll(getRolePermissions(monitor, this, PostgrePrivilegeGrant.Kind.FUNCTION, dbStat));
            } catch (Throwable e) {
                log.error("Error reading routine privileges", e);
            }
            // Select acl for all schemas, sequences and materialized views
            boolean supportsDistinct = getDataSource().getServerType().supportsDistinctForStatementsWithAcl(); // Greenplum do not support DISTINCT keyword with the acl data type in the query
            boolean supportsOnlySchemasPermissions = !getDataSource().isServerVersionAtLeast(9,0); // So we can't use aclexplode in old PG versions. Let's read only schemas permissions then
            String otherObjectsSQL;
            if (supportsOnlySchemasPermissions) {
                otherObjectsSQL = "SELECT n.oid, n.nspacl FROM pg_catalog.pg_namespace n WHERE n.nspacl IS NOT NULL";
            } else {
                otherObjectsSQL = "SELECT * FROM (\n" +
                    "\tSELECT " + (supportsDistinct ? "DISTINCT" : "") + " relnamespace,\n" +
                    "\trelacl,\n" +
                    "\trelname,\n" +
                    "\trelkind,\n" +
                    "(aclexplode(relacl)).grantee as granteeI\n" +
                    "FROM\n" +
                    "\tpg_class\n" +
                    "WHERE\n" +
                    "\trelacl IS NOT NULL\n" +
                    "\tAND relnamespace IN (\n" +
                    "SELECT oid\n" +
                    "FROM pg_namespace\n" +
                    "WHERE nspname NOT LIKE 'pg_%' AND nspname != 'information_schema')\n" +
                    "UNION ALL\n" +
                    "SELECT " + (supportsDistinct ? "DISTINCT" : "") +
                    "\n\tn.oid AS relnamespace,\n" +
                    "\tnspacl AS relacl,\n" +
                    "\tn.nspname AS relname,\n" +
                    "\tcast('C' as \"char\") AS relkind,\n" +
                    "(aclexplode(nspacl)).grantee as granteeI\n" +
                    "FROM\n" +
                    "\tpg_catalog.pg_namespace n\n" +
                    "WHERE\n" +
                    "\tn.nspacl IS NOT NULL \n" +
                    "\t) AS tr\n" +
                    "WHERE pg_get_userbyid(tr.granteeI)= ?" +
                    " AND tr.relkind IN('S', 'm', 'C')";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(otherObjectsSQL)) {
                if (!supportsOnlySchemasPermissions) {
                    dbStat.setString(1, getName());
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
                        PostgreSchema schema = getDatabase().getSchema(monitor, schemaId);
                        if (schema != null) {
                            List<PostgrePrivilege> privileges = null;
                            PostgrePrivilegeGrant.Kind pKind = null;
                            if (supportsOnlySchemasPermissions) {
                                pKind = PostgrePrivilegeGrant.Kind.SCHEMA;
                                privileges = PostgreUtils.extractPermissionsFromACL(monitor, schema, acl, false);
                            } else if (objectType != null && objectName != null) {
                                pKind = PostgrePrivilegeGrant.Kind.TABLE;
                                if (objectType.equals("C")) {
                                    privileges = PostgreUtils.extractPermissionsFromACL(monitor, schema, acl, false);
                                    pKind = PostgrePrivilegeGrant.Kind.SCHEMA;
                                } else if (PostgreClass.RelKind.S.getCode().equals(objectType)) {
                                    PostgreSequence sequence = schema.getSequence(monitor, objectName);
                                    privileges = PostgreUtils.extractPermissionsFromACL(monitor, sequence, acl, false);
                                    pKind = PostgrePrivilegeGrant.Kind.SEQUENCE;
                                } else {
                                    PostgreMaterializedView materializedView = schema.getMaterializedView(monitor, objectName);
                                    privileges = PostgreUtils.extractPermissionsFromACL(monitor, materializedView, acl, false);
                                }
                            }
                            for (PostgrePrivilege p : CommonUtils.safeCollection(privileges)) {
                                if (p instanceof PostgreObjectPrivilege) {
                                    PostgreRoleReference grantee = ((PostgreObjectPrivilege) p).getGrantee();
                                    if (grantee != null && this.isReferencedWith(grantee)) {
                                        List<PostgrePrivilegeGrant> grants = new ArrayList<>();
                                        for (PostgrePrivilege.ObjectPermission perm : p.getPermissions()) {
                                            grants.add(new PostgrePrivilegeGrant(
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
                                            new PostgreRolePrivilege(
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
                        FROM pg_default_acl a WHERE a.defaclnamespace <> 0) as g
                        where pg_get_userbyid(g.grantee) = ?""")) {
                    dbStat.setString(1, getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.nextRow()) {
                            long schemaId = JDBCUtils.safeGetLong(dbResult, "defaclnamespace");
                            PostgreSchema schema = getDatabase().getSchema(monitor, schemaId);
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
                            List<PostgrePrivilege> privileges = PostgreUtils.extractPermissionsFromACL(monitor, schema, acl, true);
                            List<PostgrePrivilege> resultPrivileges = new ArrayList<>();
                            for (PostgrePrivilege privilege : privileges) {
                                if (privilege instanceof PostgreDefaultPrivilege defaultPrivilege) {
                                    if (!defaultPrivilege.getGrantee().getRoleName().equals(getName())) {
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

    protected static Collection<PostgrePrivilege> getRolePermissions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreRole role,
        @NotNull PostgrePrivilegeGrant.Kind kind,
        @NotNull JDBCPreparedStatement dbStat) throws SQLException
    {
        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
            Map<String, List<PostgrePrivilegeGrant>> privs = new LinkedHashMap<>();
            while (dbResult.next()) {
                PostgrePrivilegeGrant privilege = new PostgrePrivilegeGrant(role.database, kind, dbResult);
                String privilegeObjectName = privilege.getObjectName();
                String objectSchema = privilege.getObjectSchema();
                if ((kind == PostgrePrivilegeGrant.Kind.FUNCTION || kind == PostgrePrivilegeGrant.Kind.PROCEDURE)
                    && CommonUtils.isNotEmpty(privilegeObjectName) && privilegeObjectName.contains("_")
                    && !privilegeObjectName.endsWith("_") && CommonUtils.isNotEmpty(objectSchema))
                {
                    changeRoutineFullName(monitor, role, privilege, privilegeObjectName, objectSchema);
                }
                String tableId = objectSchema + "." + privilege.getObjectName();
                List<PostgrePrivilegeGrant> privList = privs.computeIfAbsent(tableId, k -> new ArrayList<>());
                privList.add(privilege);
            }
            // Pack to permission list
            List<PostgrePrivilege> result = new ArrayList<>(privs.size());
            for (List<PostgrePrivilegeGrant> priv : privs.values()) {
                PostgrePrivilegeGrant privilegeGrant = priv.get(0);
                result.add(new PostgreRolePrivilege(
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
        @NotNull PostgreRole role,
        @NotNull PostgrePrivilegeGrant privilege,
        String privilegeObjectName,
        String objectSchema)
    {
        // Functions and procedures stores their names as specific name
        // Specific name = usual name + underscore + object id
        // We need to get this id and search this routine in schema
        // To get it correct full name with parameters
        String privId = privilegeObjectName.substring(privilegeObjectName.lastIndexOf("_") + 1);
        long routineId = CommonUtils.toLong(privId, -1);
        if (routineId != -1) {
            // Start searching routine in schema to get it full name and change it for PostgrePrivilegeGrant object
            PostgreDatabase database = role.getDatabase();
            PostgreSchema schema;
            try {
                schema = database.getSchema(monitor, objectSchema);
            } catch (DBException e) {
                log.debug("Can't find routine schema '" + objectSchema + "'", e);
                // We can try to use public schema in this case
                schema = database.getPublicSchema();
            }
            if (schema != null) {
                PostgreProcedure procedure = null;
                try {
                    procedure = schema.getProcedure(monitor, routineId);
                } catch (DBException e) {
                    log.debug("Can't find routine in schema '" + privilegeObjectName + "'", e);
                }
                if (procedure != null && CommonUtils.isNotEmpty(procedure.getOverloadedName())) {
                    privilege.setObjectName(procedure.getOverloadedName());
                    if (procedure.getKind() == PostgreProcedureKind.p) {
                        // They all are FUNCTIONS by default
                        privilege.setKind(PostgrePrivilegeGrant.Kind.PROCEDURE);
                    }
                }
            }
        }
    }

    /**
     * Specific tole type name for SQL statement correct generation.
     */
    @Nullable
    public String getSpecificRoleType() {
        return null;
    }

    public PostgreRoleReference getRoleReference() {
        return new PostgreRoleReference(this.database, this.getName(), this.getSpecificRoleType());
    }

    public boolean isReferencedWith(PostgreRoleReference reference) {
        return reference != null
            && Objects.equals(this.getDatabase(), reference.getDatabase())
            && Objects.equals(this.getName(), reference.getRoleName())
            && Objects.equals(this.getSpecificRoleType(), reference.getRoleType());
    }

    /**
     * Returns true if role/user/group can't see and change routines (procedures and functions) privileges.
     */
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

    public static class PostgreRoleCanBeSuperUserValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsSuperusers();
        }
    }

    public static class PostgreRoleInheritValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsInheritance();
        }
    }

    public static class PostgreRoleCanCreateDBValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRolesWithCreateDBAbility();
        }
    }

    public static class RoleCanBeReplicationValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRoleReplication();
        }
    }

    public static class RoleCanBypassRLSValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRoleBypassRLS();
        }
    }

    public static class PersistenceUserValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return !object.isPersisted();
        }
    }

    public static class CommentsOnRolesSupportedValidator implements IPropertyValueValidator<PostgreRole, Object> {
        @Override
        public boolean isValidValue(PostgreRole object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsCommentsOnRole();
        }
    }

    private static class PostgreRoleSetting {

        @Nullable PostgreDatabase database;
        @NotNull String configurationParameter;

        PostgreRoleSetting(@Nullable PostgreDatabase database, @NotNull String configurationParameter) {
            this.database = database;
            this.configurationParameter = configurationParameter;
        }
    }
}
