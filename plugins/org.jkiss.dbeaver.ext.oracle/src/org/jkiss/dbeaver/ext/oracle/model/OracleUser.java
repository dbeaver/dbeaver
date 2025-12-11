/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.internal.OracleMessages;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OracleUser
 */
public class OracleUser extends OracleGrantee implements DBAUser, DBSObjectLazy<OracleDataSource>, DBPSaveableObject, DBPRefreshableObject,
    DBPScriptObject {
    private static final Log log = Log.getLog(OracleUser.class);

    private long id;
    private String name;
    private String externalName;
    private String status;
    private boolean isLocked;
    private Timestamp createDate;
    private Timestamp lockDate;
    private Timestamp expiryDate;
    private Object defaultTablespace;
    private Object tempTablespace;
    private Object profile;
    private String consumerGroup;
    protected transient String password;
    protected transient String confirmPassword;
    private boolean persisted;

    public OracleUser(OracleDataSource dataSource) {
        super(dataSource);
    }

    public OracleUser(OracleDataSource dataSource, ResultSet resultSet) {
        super(dataSource);
        if (resultSet != null) {
            this.id = JDBCUtils.safeGetLong(resultSet, "USER_ID");
            this.name = JDBCUtils.safeGetString(resultSet, "USERNAME");
            this.externalName = JDBCUtils.safeGetString(resultSet, "EXTERNAL_NAME");
            this.status = JDBCUtils.safeGetString(resultSet, "ACCOUNT_STATUS");
            this.isLocked = status != null && status.contains("LOCKED");

            this.createDate = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
            this.lockDate = JDBCUtils.safeGetTimestamp(resultSet, "LOCK_DATE");
            this.expiryDate = JDBCUtils.safeGetTimestamp(resultSet, "EXPIRY_DATE");
            this.defaultTablespace = JDBCUtils.safeGetString(resultSet, "DEFAULT_TABLESPACE");
            this.tempTablespace = JDBCUtils.safeGetString(resultSet, "TEMPORARY_TABLESPACE");

            this.profile = JDBCUtils.safeGetString(resultSet, "PROFILE");
            this.consumerGroup = JDBCUtils.safeGetString(resultSet, "INITIAL_RSRC_CONSUMER_GROUP");
            this.persisted = true;
        } else {
            this.persisted = false;
        }
    }

    @Property(order = 1)
    public long getId() {
        return id;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Property(order = 3, visibleIf = OracleUserPropertyValidator.class)
    public String getExternalName() {
        return externalName;
    }

    @Property(viewable = true, order = 4, visibleIf = OracleUserPropertyValidator.class)
    public String getStatus() {
        return status;
    }

    @Property(viewable = true, order = 5)
    public Timestamp getCreateDate() {
        return createDate;
    }

    @Property(order = 6, visibleIf = OracleUserPropertyValidator.class)
    public Timestamp getLockDate() {
        return lockDate;
    }

    @Property(order = 7, visibleIf = OracleUserPropertyValidator.class)
    public Timestamp getExpiryDate() {
        return expiryDate;
    }

    @Property(order = 8, visibleIf = OracleUserPropertyValidator.class)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
        return OracleTablespace.resolveTablespaceReference(monitor, this, "defaultTablespace");
    }

    @Property(order = 9, visibleIf = OracleUserPropertyValidator.class)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTempTablespace(DBRProgressMonitor monitor) throws DBException {
        return OracleTablespace.resolveTablespaceReference(monitor, this, "tempTablespace");
    }

    @Nullable
    @Override
    public Object getLazyReference(Object propertyId) {
        if ("defaultTablespace".equals(propertyId)) {
            return defaultTablespace;
        } else if ("tempTablespace".equals(propertyId)) {
            return tempTablespace;
        } else if ("profile".equals(propertyId)) {
            return profile;
        } else {
            return null;
        }
    }

    @Property(order = 10, visibleIf = OracleUserPropertyValidator.class)
    @LazyProperty(cacheValidator = ProfileReferenceValidator.class)
    public Object getProfile(DBRProgressMonitor monitor) throws DBException {
        return OracleUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().profileCache, this, "profile");
    }

    @Property(order = 11, visibleIf = OracleUserPropertyValidator.class)
    public String getConsumerGroup() {
        return consumerGroup;
    }

    /**
     * Passwords are never read from database. It is used to create/alter schema/user
     *
     * @return password or null
     */
    @Property(visibleIf = OracleUserModifyValueValidator.class, editable = true, updatable = true, order = 12, password = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Property(visibleIf = OracleUserModifyValueValidator.class, editable = true, updatable = true, order = 13, password = true)
    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Property(visibleIf = OracleUserLockedValueValidator.class, editable = true, updatable = true, order = 14)
    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    @Property(visibleIf = OracleUserPropertyHiddenValidator.class, order = 15, info = true)
    public String getInfoLabel() {
        return OracleMessages.edit_oracle_user_disabled_fields_info_label;
    }

    @Override
    @Association
    public Collection<OraclePrivRole> getRolePrivs(DBRProgressMonitor monitor) throws DBException {
        return rolePrivCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return super.refreshObject(monitor);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- DROP USER ").append(DBUtils.getQuotedIdentifier(this)).append(";\n\n");

        try (JDBCSession session = DBUtils.openMetaSession(
            monitor,
            this,
            "Load definition for USER '" + this.name + "'"
        )) {
            String userDDL = OracleUtils.fetchDDL(session, "USER", this.getName());
            SQLDialect sqlDialect = getDataSource().getSQLDialect();
            OracleUtils.addDDLLine(sql, userDDL);

            if (getDataSource().isAtLeastV10()) {
                SQLUtils.addMultiStatementDDL(
                    sqlDialect,
                    sql,
                    OracleUtils.invokeDBMSMetadataGetGrantedDDL(
                        session,
                        this,
                        OracleUtils.DBMSMetaGrantedObjectType.SYSTEM_GRANT
                    )
                );

                SQLUtils.addMultiStatementDDL(
                    sqlDialect,
                    sql,
                    OracleUtils.invokeDBMSMetadataGetGrantedDDL(
                        session,
                        this,
                        OracleUtils.DBMSMetaGrantedObjectType.ROLE_GRANT
                    )
                );

                SQLUtils.addMultiStatementDDL(
                    sqlDialect,
                    sql,
                    OracleUtils.invokeDBMSMetadataGetGrantedDDL(
                        session,
                        this,
                        OracleUtils.DBMSMetaGrantedObjectType.OBJECT_GRANT
                    )
                );

                // PL/SQL-block
                OracleUtils.addDDLLine(
                    sql,
                    OracleUtils.invokeDBMSMetadataGetGrantedDDL(
                        session,
                        this,
                        OracleUtils.DBMSMetaGrantedObjectType.TABLESPACE_QUOTA
                    )
                );

                appendDefaultRolesDDL(monitor, sql);
            }
        } catch (SQLException e) {
            throw new DBException("Failed of getting Oracle user definition", e);
        }

        return sql.toString();
    }

    public static class ProfileReferenceValidator implements IPropertyCacheValidator<OracleUser> {
        @Override
        public boolean isPropertyCached(@NotNull OracleUser object, @NotNull Object propertyId) {
            return
                object.getLazyReference(propertyId) instanceof OracleUserProfile ||
                    object.getLazyReference(propertyId) == null ||
                    object.getDataSource().profileCache.isFullyCached();
        }
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    public static class OracleUserModifyValueValidator implements IPropertyValueValidator<OracleUser, Object> {
        @Override
        public boolean isValidValue(@NotNull OracleUser object, @Nullable Object value) throws IllegalArgumentException {
            return object.getDataSource().supportsUserEdit();
        }
    }

    public static class OracleUserLockedValueValidator implements IPropertyValueValidator<OracleUser, Object> {
        @Override
        public boolean isValidValue(@NotNull OracleUser object, @Nullable Object value) throws IllegalArgumentException {
            return object.getDataSource().supportsUserEdit() && CommonUtils.toBoolean(object.getDataSource().getContainer().getConnectionConfiguration()
                .getProviderProperty(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS));
        }
    }

    public static class OracleUserPropertyValidator implements IPropertyValueValidator<OracleUser, Object> {
        @Override
        public boolean isValidValue(@NotNull OracleUser object, @Nullable Object value) throws IllegalArgumentException {
            return CommonUtils.toBoolean(object.getDataSource().getContainer().getConnectionConfiguration()
                .getProviderProperty(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS));
        }
    }

    public static class OracleUserPropertyHiddenValidator implements IPropertyValueValidator<OracleUser, Object> {
        @Override
        public boolean isValidValue(@NotNull OracleUser object, @Nullable Object value) throws IllegalArgumentException {
            return !CommonUtils.toBoolean(object.getDataSource().getContainer().getConnectionConfiguration()
                .getProviderProperty(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS));
        }
    }

    private void appendDefaultRolesDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder sql
    ) throws DBException {
        Collection<OraclePrivRole> rolePrivs = getRolePrivs(monitor);
        List<String> defaultRoles = new ArrayList<>();

        for (OraclePrivRole rolePriv : rolePrivs) {
            if (rolePriv.isDefaultRole()) {
                Object role = rolePriv.getRole(monitor);
                if (role instanceof OracleRole oracleRole) {
                    defaultRoles.add(oracleRole.getName());
                }
            }
        }

        if (defaultRoles.isEmpty()) {
            return;
        }

        sql.append("\nALTER USER ")
            .append(DBUtils.getQuotedIdentifier(getDataSource(), getName()))
            .append(" DEFAULT ROLE ");

        for (int i = 0; i < defaultRoles.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(DBUtils.getQuotedIdentifier(getDataSource(), defaultRoles.get(i)));
        }

        sql.append(";\n");
    }

}
