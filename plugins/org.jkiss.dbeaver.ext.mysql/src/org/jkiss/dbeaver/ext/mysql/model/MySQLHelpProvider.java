/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLHelpTopic;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MySQLHelpProvider
 */
public class MySQLHelpProvider implements SQLHelpProvider
{
    private static final Log log = Log.getLog(MySQLHelpProvider.class);

    private final MySQLDataSource dataSource;
    private final Map<String, SQLHelpTopic> topicCache = new HashMap<>();
    private boolean isLoaded = false;

    public MySQLHelpProvider(MySQLDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public SQLHelpTopic findHelpTopic(DBRProgressMonitor monitor, String keyword, DBPKeywordType keywordType) {
        return selectHelpTopic(monitor, keyword);
    }

    private SQLHelpTopic selectHelpTopic(DBRProgressMonitor monitor, String topic) {
        if (CommonUtils.isEmpty(topic)) {
            return null;
        }
        if (!isLoaded) {
            loadTopics(monitor);
        }
        synchronized (topicCache) {
            return topicCache.get(topic.toUpperCase(Locale.ENGLISH));
        }
    }

    private void loadTopics(DBRProgressMonitor monitor) {
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read MySQL help topics")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT name, description, example, url FROM mysql.help_topic")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String topicName = dbResult.getString(1);
                        SQLHelpTopic helpTopic = new SQLHelpTopic();
                        helpTopic.setContents("<pre>" + dbResult.getString(2) + "</pre>");
                        helpTopic.setExample(dbResult.getString(3));
                        helpTopic.setUrl(dbResult.getString(4));
                        if (topicName != null) {
                            synchronized (topicCache) {
                                topicCache.put(topicName.toUpperCase(Locale.ENGLISH), helpTopic);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.debug("Error reading help topics: " + e.getMessage());
            }
        }
        finally {
            isLoaded = true;
        }
    }

}
