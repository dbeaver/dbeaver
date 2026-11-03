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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SQLServerWindowsAccount {
    public enum CheckResult {
        AVAILABLE, LOGIN_EXISTS, NOT_FOUND
    }

    private SQLServerWindowsAccount() {
    }

    @NotNull
    public static List<String> readKnownDomains(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerDataSource dataSource)
            throws DBException {
        List<String> domains = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read Windows login domains");
             JDBCPreparedStatement statement = session.prepareStatement(
                 "SELECT DISTINCT LEFT(name, CHARINDEX(N'\\', name) - 1) FROM sys.server_principals"
                     + " WHERE type IN ('U', 'G') AND CHARINDEX(N'\\', name) > 1 ORDER BY 1");
             JDBCResultSet result = statement.executeQuery()) {
            while (result.next()) {
                domains.add(result.getString(1));
            }
        } catch (SQLException e) {
            throw new DBException("Cannot read domains from existing Windows logins", e);
        }
        return domains;
    }

    @NotNull
    public static CheckResult checkName(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SQLServerDataSource dataSource,
        @NotNull String name
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Check Windows account");
             JDBCPreparedStatement statement = session.prepareStatement("SELECT SUSER_SID(?)")) {
            statement.setString(1, name);
            byte[] sid;
            try (JDBCResultSet result = statement.executeQuery()) {
                if (!result.next() || (sid = result.getBytes(1)) == null) {
                    return CheckResult.NOT_FOUND;
                }
            }
            try (JDBCPreparedStatement existing = session.prepareStatement("SELECT name FROM sys.server_principals WHERE sid = ?")) {
                existing.setBytes(1, sid);
                try (JDBCResultSet result = existing.executeQuery()) {
                    return result.next() ? CheckResult.LOGIN_EXISTS : CheckResult.AVAILABLE;
                }
            }
        } catch (SQLException e) {
            throw new DBException("SQL Server could not resolve the Windows account", e);
        }
    }
}
