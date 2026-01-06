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
package org.jkiss.dbeaver.ext.oracle.model.session;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.internal.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.admin.sessions.*;
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
 * Oracle session manager
 */
public class OracleServerSessionManager implements DBAServerSessionManager<OracleServerSession>, DBAServerSessionManagerSQL, DBAServerSessionDetailsProvider {

    public static final String PROP_KILL_SESSION = "killSession";
    public static final String PROP_IMMEDIATE = "immediate";

    public static final String OPTION_SHOW_BACKGROUND = "showBackground";
    public static final String OPTION_SHOW_INACTIVE = "showInactive";

    private final OracleDataSource dataSource;

    public OracleServerSessionManager(OracleDataSource dataSource)
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
    public Collection<OracleServerSession> getSessions(@NotNull DBCSession session, @NotNull Map<String, Object> options) throws DBException {
        try {

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(generateSessionReadQuery(options))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<OracleServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new OracleServerSession(dbResult));
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
        final boolean toKill = Boolean.TRUE.equals(options.get(PROP_KILL_SESSION));
        final boolean immediate = Boolean.TRUE.equals(options.get(PROP_IMMEDIATE));

        try {
            StringBuilder sql = new StringBuilder("ALTER SYSTEM ");
            if (toKill) {
                sql.append("KILL SESSION ");
            } else {
                sql.append("DISCONNECT SESSION ");
            }
            sql.append("'").append(sessionId).append("'");
            if (immediate) {
                sql.append(" IMMEDIATE");
            } else if (!toKill) {
                sql.append(" POST_TRANSACTION");
            }
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
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
        return Map.of(OracleServerSessionManager.PROP_KILL_SESSION, true);
    }

    @NotNull
    @Override
    public List<DBAServerSessionDetails> getSessionDetails() {
        List<DBAServerSessionDetails> extDetails = new ArrayList<>();
        extDetails.add(new AbstractServerSessionDetails(
            OracleMessages.oracle_server_session_manager_details_name,
            OracleMessages.oracle_server_session_manager_details_description,
            DBIcon.TYPE_DATETIME
        ) {
            @Override
            public List<OracleServerLongOp> getSessionDetails(@NotNull DBCSession session, @NotNull DBAServerSession serverSession) throws DBException {
                try {
                    try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                        "SELECT * FROM GV$SESSION_LONGOPS WHERE INST_ID=? AND SID=? AND SERIAL#=?"))
                    {
                        dbStat.setLong(1, ((OracleServerSession) serverSession).getInstId());
                        dbStat.setLong(2, ((OracleServerSession) serverSession).getSid());
                        dbStat.setLong(3, ((OracleServerSession) serverSession).getSerial());
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            List<OracleServerLongOp> longOps = new ArrayList<>();
                            while (dbResult.next()) {
                                longOps.add(new OracleServerLongOp(dbResult));
                            }
                            return longOps;
                        }
                    }
                } catch (SQLException e) {
                    throw new DBDatabaseException(e, session.getDataSource());
                }
            }

            @Override
            public Class<? extends DBPObject> getDetailsType() {
                return OracleServerLongOp.class;
            }
        });
        extDetails.add(new AbstractServerSessionDetails(
            OracleMessages.oracle_server_session_manager_display_exec_plan_name,
            OracleMessages.oracle_server_session_manager_display_exec_plan_description,
            DBIcon.TYPE_TEXT
        ) {
            @Override
            public List<OracleServerExecutePlan> getSessionDetails(@NotNull DBCSession session, @NotNull DBAServerSession serverSession) throws DBException {
                try {
                    try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                        "SELECT PLAN_TABLE_OUTPUT FROM TABLE(dbms_xplan.display_cursor(sql_id => ?, cursor_child_no => ?))"))
                    {
                        dbStat.setString(1, ((OracleServerSession) serverSession).getSqlId());
                        dbStat.setLong(2, ((OracleServerSession) serverSession).getSqlChildNumber());
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) 
                        {
							List<OracleServerExecutePlan> planItems = new ArrayList<>();
							while (dbResult.next()) {
                                planItems.add(new OracleServerExecutePlan(dbResult));
                            }
							return planItems;
						}
                    }							
                } catch (SQLException e) {
                    throw new DBDatabaseException(e, session.getDataSource());
                }
            }

            @Override
            public Class<? extends DBPObject> getDetailsType() {
                return OracleServerExecutePlan.class;
            }
        });      
        return extDetails;
    }

    @Override
    public boolean canGenerateSessionReadQuery() {
        return true;
    }

    @NotNull
    @Override
    public String generateSessionReadQuery(@NotNull Map<String, Object> options) {
        boolean atLeastV11 = dataSource.isAtLeastV11();

        StringBuilder sql = new StringBuilder();
        sql.append(
            "SELECT s.*, ");
        if (atLeastV11) {
            sql.append("(SELECT SQL_FULLTEXT FROM gv$sql vsql\n" +
                "WHERE s.sql_address = vsql.address(+) AND s.sql_hash_value = vsql.hash_value(+)\n" +
                "AND s.sql_child_number = vsql.child_number (+)) as  SQL_FULLTEXT, ");
        } else {
            sql.append("sq.SQL_TEXT AS SQL_FULLTEXT, ");
        }
        sql.append("io.*\n" +
            "FROM GV$SESSION s, gv$sql sq, gv$sess_io io\n" +
            "WHERE s.sql_address = sq.address(+)\n" +
            " AND s.sql_hash_value = sq.hash_value(+)" +
            " AND s.sid = io.sid(+)" +
            " AND s.inst_id = io.inst_id(+)");
        //"LEFT JOIN v$sesstat stat ON ( s.sid = stat.sid)\n" +
        //"LEFT OUTER JOIN v$process e ON (s.paddr = e.addr)\n" +
        //"WHERE 1=1");
        if (atLeastV11) {
            sql.append(" AND s.sql_child_number = sq.child_number (+)");
        }
        if (!CommonUtils.getOption(options, OPTION_SHOW_BACKGROUND)) {
            sql.append(" AND s.TYPE = 'USER'");
        }
        if (!CommonUtils.getOption(options, OPTION_SHOW_INACTIVE)) {
            sql.append(" AND s.STATUS <> 'INACTIVE'");
        }
        return sql.toString();
    }
}
