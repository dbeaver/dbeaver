/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.util.Date;

public class SQLServerLogin implements DBAUser, DBPStatefulObject {

    public enum LoginType {
        S("SQL Server login"),
        U("Windows login"),
        G("Windows group"),
        R("Server role"),
        C("Login mapped to a certificate"),
        E("External Login"), // from Azure Active Directory
        X("External group"), // from Azure Active Directory group or applications
        K("Login mapped to an asymmetric key");

        private final String loginType;

        LoginType(String loginType) {
            this.loginType = loginType;
        }

        public String getLoginType() {
            return loginType;
        }
    }

    private final SQLServerDataSource dataSource;
    @NotNull private String loginName;
    private long loginId;
    private LoginType loginType;
    private Date createDate;
    private Date modifyDate;
    @Nullable private SQLServerDatabase defaultDatabase;
    @Nullable private String defaultLanguageName;
    private boolean isDisabled;
    private boolean isFixedRole;

    private String password; // Added through the creation dialog

    public SQLServerLogin(@NotNull SQLServerDataSource dataSource, @NotNull String loginName, @NotNull JDBCResultSet resultSet) {
        this.dataSource = dataSource;
        this.loginName = loginName;
        this.loginId = JDBCUtils.safeGetLong(resultSet, "principal_id");
        String loginTypeChar = JDBCUtils.safeGetString(resultSet, "type");
        if (CommonUtils.isNotEmpty(loginTypeChar)) {
            this.loginType = CommonUtils.valueOf(LoginType.class, loginTypeChar);
        }
        this.createDate = JDBCUtils.safeGetDate(resultSet, "create_date");
        this.modifyDate = JDBCUtils.safeGetDate(resultSet, "modify_date");
        String defaultDatabaseName = JDBCUtils.safeGetString(resultSet, "default_database_name");
        if (CommonUtils.isNotEmpty(defaultDatabaseName)) {
            SQLServerDatabase database = dataSource.getDatabase(defaultDatabaseName);
            if (database != null) {
                defaultDatabase = database;
            }
        }
        this.defaultLanguageName = JDBCUtils.safeGetString(resultSet, "default_language_name");
        this.isDisabled = JDBCUtils.safeGetBoolean(resultSet, "is_disabled");
        this.isFixedRole = JDBCUtils.safeGetBoolean(resultSet, "is_fixed_role");
    }

    // Auxiliary constructor for creating via UI-configurator
    public SQLServerLogin(SQLServerDataSource dataSource, @NotNull String loginName) {
        this.dataSource = dataSource;
        this.loginName = loginName;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return loginName;
    }

    public void setLoginName(@NotNull String loginName) {
        this.loginName = loginName;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Property(viewable = true, order = 2)
    public long getLoginId() {
        return loginId;
    }

    @Property(viewable = true, order = 3)
    public String getLoginTypeName() {
        return loginType.getLoginType();
    }

    @Property(viewable = true, order = 4)
    public Date getCreateDate() {
        return createDate;
    }

    @Property(viewable = true, order = 5)
    public Date getModifyDate() {
        return modifyDate;
    }

    @Nullable
    @Property(viewable = true, order = 6)
    public SQLServerDatabase getDefaultDatabase() {
        return defaultDatabase;
    }

    @Nullable
    @Property(viewable = true, order = 7)
    public String getDefaultLanguageName() {
        return defaultLanguageName;
    }

    @Property(viewable = true, updatable = true, order = 8)
    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    @Property(viewable = true, order = 9)
    public boolean isFixedRole() {
        return isFixedRole;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if (isDisabled) {
            return DBSObjectState.INVALID;
        }
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
    }
}
