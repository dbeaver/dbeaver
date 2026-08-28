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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleGrantee;
import org.jkiss.dbeaver.ext.oracle.model.OracleRole;
import org.jkiss.dbeaver.ext.oracle.model.OracleUser;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TiberoDataSource
 */
public class TiberoDataSource extends OracleDataSource {

    public TiberoDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException {
        super(monitor, container, new TiberoSQLDialect());
    }

    @NotNull
    @Override
    public TiberoSchema createSchemaImpl(@NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) {
        return new TiberoSchema(this, resultSet);
    }

    @Override
    protected void initializeContextState(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        @Nullable JDBCExecutionContext initFrom
    ) throws DBException {
        // Keep Tibero initialization aligned with Oracle, but skip Oracle-only
        // optimizer session parameters that Tibero does not recognize.
        context.getContextDefaults().refreshDefaults(monitor, true);
    }

    @NotNull
    @Override
    @Association
    public Collection<OracleUser> getUsers(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero users");
             JDBCPreparedStatement statement = session.prepareStatement(
                 "SELECT ROWNUM AS USER_ID, u.USERNAME, " +
                     "CAST(NULL AS VARCHAR(255)) AS EXTERNAL_NAME, " +
                     "CAST(NULL AS VARCHAR(255)) AS ACCOUNT_STATUS, " +
                     "CAST(NULL AS TIMESTAMP) AS LOCK_DATE, " +
                     "CAST(NULL AS TIMESTAMP) AS EXPIRY_DATE, " +
                     "CAST(NULL AS VARCHAR(255)) AS DEFAULT_TABLESPACE, " +
                     "CAST(NULL AS VARCHAR(255)) AS TEMPORARY_TABLESPACE, " +
                     "CAST(NULL AS VARCHAR(255)) AS PROFILE, " +
                     "CAST(NULL AS VARCHAR(255)) AS INITIAL_RSRC_CONSUMER_GROUP, " +
                     "CAST(NULL AS TIMESTAMP) AS CREATED " +
                 "FROM (SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME) u")) {
            List<OracleUser> users = new ArrayList<>();
            try (JDBCResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(new OracleUser(this, resultSet));
                }
            }
            return users;
        } catch (SQLException e) {
            throw new DBException("Failed to load Tibero users", e);
        }
    }

    @Nullable
    @Override
    @Association
    public OracleUser getUser(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        for (OracleUser user : getUsers(monitor)) {
            if (user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public OracleGrantee getGrantee(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        OracleUser user = getUser(monitor, name);
        if (user != null) {
            return user;
        }
        for (OracleRole role : getRoles(monitor)) {
            if (role.getName().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}
