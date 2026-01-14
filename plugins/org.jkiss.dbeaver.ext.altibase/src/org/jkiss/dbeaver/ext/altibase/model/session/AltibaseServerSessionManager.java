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
package org.jkiss.dbeaver.ext.altibase.model.session;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManagerSQL;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Altibase session manager
 */
public class AltibaseServerSessionManager implements DBAServerSessionManager<AltibaseServerSession>, DBAServerSessionManagerSQL {

    private final AltibaseDataSource dataSource;

    public AltibaseServerSessionManager(AltibaseDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public Collection<AltibaseServerSession> getSessions(@NotNull DBCSession session, 
            @NotNull Map<String, Object> options) throws DBException {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(generateSessionReadQuery(options))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<AltibaseServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new AltibaseServerSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(@NotNull DBCSession session, @NotNull String sessionId, 
            @NotNull Map<String, Object> options) throws DBException {
        try {

            String sql = String.format("ALTER DATABASE %s SESSION CLOSE %s",
                    dataSource.getDbName((JDBCSession) session), 
                    sessionId);
            
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                dbStat.execute();
            }
        } catch (SQLException e) {
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
        return "SELECT"
                + " s.id session_id"
                + " , s.trans_id tx_id"
                + " , CURRENT_STMT_ID stmt_id"
                + " , db_username user_name"
                + " , nvl2(s.query, s.query, ' ') AS sql"
                + " , obj_name lock_target"
                + " , DECODE(is_grant, 1, 'HOLDER', 0, 'WAITER', '') lock_status"
                + " , lock_desc lock_type"
                + " , TO_CHAR(conv_timezone(UNIX_TO_DATE( login_time ), '+00:00', db_timezone())," 
                    + " 'YYYY-MM-DD HH24:MI:SS') login_time"
                + " , CASE2(IDLE_START_TIME < 1, '',"
                    + " TO_CHAR(conv_timezone(UNIX_TO_DATE( idle_start_time ), '+00:00'," 
                    + " db_timezone()), 'YYYY-MM-DD HH24:MI:SS')) idle_since"
                + " , CASE2(autocommit_flag = 1, 'T', 'F') autocommit"
                + " , decode(sysdba_flag, 0, 'F', 1, 'T') sysda"
                + " , query_time_limit"
                + " , ddl_time_limit"
                + " , fetch_time_limit"
                + " , utrans_time_limit"
                + " , idle_time_limit"
                + " , nls_territory"
                + " , time_zone"
                + " , client_app_info"
                + " , comm_name"
                + " , client_type"
                + " , client_protocol_version"
                + " , client_pid"
            + " FROM "
                + " (SELECT"
                    + " ss.*, st.query"
                 + " FROM"
                    + " v$session ss LEFT OUTER JOIN v$statement st "
                    + " ON st.session_id = ss.id AND st.tx_id = ss.trans_id AND st.id = ss.current_stmt_id) s"
                 + " LEFT OUTER JOIN"
                + " (SELECT "
                    + " u.user_name || '.' ||a.table_name obj_name"
                    + " , b.trans_id "
                    + " , b.lock_desc "
                    + " , b.is_grant "
                + " FROM "
                    + " system_.sys_tables_ a, v$lock b, system_.sys_users_ u "
                + " WHERE "
                    + " u.user_id = a.user_id"
                    + " AND a.table_oid = b.table_oid) l ON s.trans_id = l.trans_id"
            + " ORDER BY login_time";
    }
}
