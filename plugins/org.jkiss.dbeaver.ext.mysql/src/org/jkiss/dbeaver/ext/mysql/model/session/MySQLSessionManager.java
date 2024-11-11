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
package org.jkiss.dbeaver.ext.mysql.model.session;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManagerSQL;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MySQL session manager
 */
public class MySQLSessionManager implements DBAServerSessionManager<MySQLSession>, DBAServerSessionManagerSQL {

    public static final String PROP_KILL_QUERY = "killQuery";

    public static final String OPTION_HIDE_SLEEPING = "hideSleeping";
    public static final String OPTION_SHOW_PERFORMANCE = "showPerformance";

    private final MySQLDataSource dataSource;

    public MySQLSessionManager(MySQLDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public Collection<MySQLSession> getSessions(@NotNull DBCSession session, @NotNull Map<String, Object> options) throws DBException
    {
        boolean hideSleeping = CommonUtils.getOption(options, OPTION_HIDE_SLEEPING);
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(generateSessionReadQuery(options))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        MySQLSession sessionInfo = new MySQLSession(dbResult, options);
                        if (hideSleeping && "Sleep".equals(sessionInfo.getCommand())) {
                            continue;
                        }
                        sessions.add(sessionInfo);
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(@NotNull DBCSession session, @NotNull String sessionId, @NotNull Map<String, Object> options) throws DBException
    {
        try {
            String sqlCommand = Boolean.TRUE.equals(options.get(PROP_KILL_QUERY)) ?
                "KILL QUERY ?" :
                "KILL CONNECTION ?";

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sqlCommand)) {
                dbStat.setString(1, sessionId);
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }

    @NotNull
    @Override
    public Map<String, Object> getTerminateOptions() {
        return Map.of();
    }

    @Override
    public boolean canGenerateSessionReadQuery() {
        return true;
    }

    @NotNull
    @Override
    public String generateSessionReadQuery(@NotNull Map<String, Object> options) {
        if (dataSource.supportsSysSchema() && CommonUtils.toBoolean(options.get(OPTION_SHOW_PERFORMANCE))) {
            return "SELECT\n" +
                "\tip.*,\n" +
                "\tsp.statement_latency,\n" +
                "\tsp.progress,\n" +
                "\tsp.lock_latency,\n" +
                "\tsp.rows_examined,\n" +
                "\tsp.rows_sent,\n" +
                "\tsp.rows_affected,\n" +
                "\tsp.tmp_tables,\n" +
                "\tsp.tmp_disk_tables,\n" +
                "\tsp.full_scan,\n" +
                "\tsp.last_statement,\n" +
                "\tsp.last_statement_latency,\n" +
                "\tsp.current_memory,\n" +
                "\tsp.source,\n" +
                "\tsp.trx_latency,\n" +
                "\tsp.trx_state,\n" +
                "\tsp.trx_autocommit,\n" +
                "\tsp.program_name\n" +
                "FROM information_schema.PROCESSLIST ip\n" +
                "LEFT JOIN sys.processlist sp ON\n" +
                "\tsp.CONN_ID = ip.ID";
        }
        return "SHOW FULL PROCESSLIST";
    }
}
