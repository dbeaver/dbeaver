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
package org.jkiss.dbeaver.ext.yashandb.model.session;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.session.OracleServerSessionManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetails;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetailsProvider;
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
 * YashanDBServerSessionManager
 */
public class YashanDBServerSessionManager
    implements DBAServerSessionManager<YashanDBServerSession>, DBAServerSessionManagerSQL, DBAServerSessionDetailsProvider {

    private final OracleServerSessionManager oracleServerSessionManager;

    public YashanDBServerSessionManager(OracleDataSource dataSource) {
        this.oracleServerSessionManager = new OracleServerSessionManager(dataSource);
    }

    @Override
    public String generateSessionReadQuery(@NotNull Map<String, Object> options) {
        // YashanDB custom
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.*, sq.SQL_TEXT SQL_TEXT,sq.CHILD_NUMBER SQL_CHILD_NUMBER\n"
            + "FROM (SELECT * FROM V$SESSION s WHERE 1=1 ");
        if (!CommonUtils.getOption(options, OracleServerSessionManager.OPTION_SHOW_BACKGROUND)) {
            sql.append(" AND s.TYPE = 'USER'");
        }
        if (!CommonUtils.getOption(options, OracleServerSessionManager.OPTION_SHOW_INACTIVE)) {
            sql.append(" AND s.STATUS <> 'INACTIVE'");
        }
        sql.append(") s LEFT join\n (SELECT * FROM v$sql WHERE PARSING_SCHEMA_NAME='SYS') sq \n"
            + " on s.sql_hash_value = sq.hash_value");
        return sql.toString();
    }

    @Override
    public List<DBAServerSessionDetails> getSessionDetails() {
        // YashanDB not support yet
        return null;
    }

    @Override
    public boolean canGenerateSessionReadQuery() {
        return oracleServerSessionManager.canGenerateSessionReadQuery();
    }

    @Override
    public DBPDataSource getDataSource() {
        return oracleServerSessionManager.getDataSource();
    }

    @Override
    public Collection<YashanDBServerSession> getSessions(@NotNull DBCSession session, @NotNull Map<String, Object> options)
            throws DBException {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(generateSessionReadQuery(options))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<YashanDBServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new YashanDBServerSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(@NotNull DBCSession session, @NotNull String sessionId, @NotNull Map<String, Object> options)
            throws DBException {
        oracleServerSessionManager.alterSession(session, sessionId, options);
    }

    @Override
    public Map<String, Object> getTerminateOptions() {
        return oracleServerSessionManager.getTerminateOptions();
    }
}
