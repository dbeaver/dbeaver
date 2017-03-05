/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLHelpTopic;

import java.sql.SQLException;

/**
 * MySQLHelpProvider
 */
public class MySQLHelpProvider implements SQLHelpProvider
{
    private static final Log log = Log.getLog(MySQLHelpProvider.class);

    private final MySQLDataSource dataSource;

    public MySQLHelpProvider(MySQLDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public SQLHelpTopic findKeywordHelp(DBRProgressMonitor monitor, String keyword) {
        return selectHelpTopic(monitor, keyword);
    }

    @Override
    public SQLHelpTopic findProcedureHelp(DBRProgressMonitor monitor, String procedure) {
        return selectHelpTopic(monitor, procedure);
    }

    @Override
    public SQLHelpTopic findTypeHelp(DBRProgressMonitor monitor, String typeName) {
        return selectHelpTopic(monitor, typeName);
    }

    private SQLHelpTopic selectHelpTopic(DBRProgressMonitor monitor, String topic) {
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read MySQL help topic")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT description, example, url FROM mysql.help_topic WHERE name=?")) {
                dbStat.setString(1, topic);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        SQLHelpTopic helpTopic = new SQLHelpTopic();
                        helpTopic.setContents("<pre>" + dbResult.getString(1) + "</pre>");
                        helpTopic.setExample(dbResult.getString(2));
                        helpTopic.setUrl(dbResult.getString(3));
                        return helpTopic;
                    }
                }
            } catch (SQLException e) {
                log.error("Error reading help topic", e);
            }
            return null;
        }
    }

}
