/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 *  Creates and executes statement for the current user password changing
 */
public class MySQLChangeUserPasswordManager implements DBAUserPasswordManager {

    private static final Log log = Log.getLog(MySQLChangeUserPasswordManager.class);

    private final MySQLDataSource dataSource;

    MySQLChangeUserPasswordManager(MySQLDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void changeUserPassword(DBRProgressMonitor monitor, String userName, String newPassword, String oldPassword) throws DBException {
        String fullUserName = null;
        // First of all we need to know our current user full name. Like 'test@localhost'
        // Because we can have user 'test@%' with localhost in the connection settings. And user 'test@%' != 'test@localhost'
        // We need to ask database directly to avoid such confusing situation
        try (JDBCSession session = DBUtils.openUtilSession(monitor, dataSource, "Read current user full name")) {
            // We use split function here because default response looks like test@localhost,
            // But we need: 'test'@'localhost' (quoted)
            // Also we need to escape special characters like in 'test'quote_user2@%'
            String currentUserName = JDBCUtils.queryString(session, "SELECT CURRENT_USER()");
            if (CommonUtils.isNotEmpty(currentUserName) && currentUserName.contains("@")) {
                String[] strings = currentUserName.split("@");
                if (strings.length == 2) {
                    fullUserName = SQLUtils.quoteString(dataSource, strings[0]) + "@" + SQLUtils.quoteString(dataSource, strings[1]);
                }
            }
        } catch (Exception e) {
            log.debug("Error reading current user info.", e);
        }
        if (CommonUtils.isEmpty(fullUserName)) {
            // If database can't return to us the full name of the current user - then generate it from our info
            String hostName = dataSource.getContainer().getConnectionConfiguration().getHostName();
            fullUserName = SQLUtils.quoteString(dataSource, userName) + "@" + SQLUtils.quoteString(dataSource, hostName);
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Change current user password")) {
            session.enableLogging(false);
            String sql;
            if (MySQLUtils.isAlterUSerSupported(dataSource)) {
                sql = "ALTER USER " + fullUserName + " IDENTIFIED BY "
                    + SQLUtils.quoteString(dataSource, CommonUtils.notEmpty(newPassword));
            } else {
                sql = "SET PASSWORD FOR " + fullUserName + " = PASSWORD(" + SQLUtils.quoteString(dataSource, newPassword) + ")";
            }
            JDBCUtils.executeSQL(session, sql);
        } catch (SQLException e) {
            throw new DBCException("Error changing current user password.", e);
        }
    }
}
