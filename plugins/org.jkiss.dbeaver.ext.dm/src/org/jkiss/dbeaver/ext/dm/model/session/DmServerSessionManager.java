package org.jkiss.dbeaver.ext.dm.model.session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.admin.sessions.AbstractServerSessionDetails;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetails;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetailsProvider;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.utils.CommonUtils;


/**
 * 此处查询的SQL使用以下SQL来进行修改
 * SELECT * FROM V$SESSION_HISTORY
 * SELECT * FROM V$SESSION_EVENT
 * select name from sysobjects where name like '%SESS%'
   select name from sysobjects where name like '%SQL%'
 * @author saorionesan
 *
 */
public class DmServerSessionManager implements DBAServerSessionManager<DmServerSession>, DBAServerSessionDetailsProvider {

    public static final String PROP_KILL_SESSION = "killSession";
    public static final String PROP_IMMEDIATE = "immediate";

    public static final String OPTION_SHOW_BACKGROUND = "showBackground";
    public static final String OPTION_SHOW_INACTIVE = "showInactive";

    private final DmDataSource dataSource;

    public DmServerSessionManager(DmDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public Collection<DmServerSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException
    {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT\r\n" + 
            		"	S.SESS_ID AS SID,\r\n" + 
            		"	S.SESS_SEQ AS INST_ID,\r\n" + 
            		"	SQL_TEXT AS SQL_TEXT,\r\n" + 
            		"	USER_NAME AS USERNAME,\r\n" + 
            		"	CURR_SCH AS SCHEMANAME,\r\n" + 
            		"	CREATE_TIME AS LOGON_TIME,\r\n" + 
            		"	\"USER\" AS TYPE,\r\n" + 
            		"	STATE AS STATE,\r\n" + 
            		"	RUN_STATUS AS STATUS,\r\n" + 
            		"	CLNT_HOST AS OSUSER,\r\n" + 
            		"	CLNT_IP AS MACHINE,\r\n" + 
            		"	APPNAME AS  MODULE,\r\n" + 
            		"	APPNAME AS  PROGRAM,\r\n" + 
            		"	OSNAME AS CLIENT_INFO\r\n" + 
            		"FROM\r\n" + 
            		"	SYS.V$SESSIONS S\r\n" + 
            		"WHERE\r\n" + 
            		"	1 = 1 ");
            if (!CommonUtils.getOption(options, OPTION_SHOW_BACKGROUND)) {
            }
            if (!CommonUtils.getOption(options, OPTION_SHOW_INACTIVE)) {
                sql.append(" AND S.STATE = 'ACTIVE'");
            }
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<DmServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new DmServerSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, DmServerSession sessionType, Map<String, Object> options) throws DBException
    {
        final boolean toKill = Boolean.TRUE.equals(options.get(PROP_KILL_SESSION));
        final boolean immediate = Boolean.TRUE.equals(options.get(PROP_IMMEDIATE));

        try {
            StringBuilder sql = new StringBuilder();
            if (toKill) {
                sql.append("call sp_close_session(").append(sessionType.getSid()).append(")");
            }
            if (immediate) {
            	 sql.append("call sp_close_session(").append(sessionType.getSid()).append(")");
            }
            
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public List<DBAServerSessionDetails> getSessionDetails() {
/*        List<DBAServerSessionDetails> extDetails = new ArrayList<>();
        extDetails.add(new AbstractServerSessionDetails("Long Operations", "Displays the status of various operations that run for longer than 6 seconds (in absolute time)", DBIcon.TYPE_DATETIME) {
            @Override
            public List<DmServerLongOp> getSessionDetails(DBCSession session, DBAServerSession serverSession) throws DBException {
                try {
                    try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                        "SELECT * FROM GV$SESSION_LONGOPS WHERE INST_ID=? AND SID=? AND SERIAL#=?"))
                    {
                        dbStat.setLong(1, ((DmServerSession) serverSession).getInstId());
                        dbStat.setLong(2, ((DmServerSession) serverSession).getSid());
                        dbStat.setLong(3, ((DmServerSession) serverSession).getSerial());
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            List<DmServerLongOp> longOps = new ArrayList<>();
                            while (dbResult.next()) {
                                longOps.add(new DmServerLongOp(dbResult));
                            }
                            return longOps;
                        }
                    }
                } catch (SQLException e) {
                    throw new DBException(e, session.getDataSource());
                }
            }

            @Override
            public Class<? extends DBPObject> getDetailsType() {
                return DmServerLongOp.class;
            }
        });
        extDetails.add(new AbstractServerSessionDetails("Display Exec Plan", "Displays execute plan from dbms_xplan by SqlId and ChildNumber", DBIcon.TYPE_TEXT) {
            @Override
            public List<DmServerExecutePlan> getSessionDetails(DBCSession session, DBAServerSession serverSession) throws DBException {
                try {
                    try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                        "SELECT PLAN_TABLE_OUTPUT FROM TABLE(dbms_xplan.display_cursor(sql_id => ?, cursor_child_no => ?))"))
                    {
                        dbStat.setString(1, ((DmServerSession) serverSession).getSqlId());
                        dbStat.setLong(2, ((DmServerSession) serverSession).getSqlChildNumber());
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) 
                        {
							List<DmServerExecutePlan> planItems = new ArrayList<>();
							while (dbResult.next()) {
                                planItems.add(new DmServerExecutePlan(dbResult));
                            }
							return planItems;
						}
                    }							
                } catch (SQLException e) {
                    throw new DBException(e, session.getDataSource());
                }
            }

            @Override
            public Class<? extends DBPObject> getDetailsType() {
                return DmServerExecutePlan.class;
            }
        });      
        return extDetails;*/
    	return null;
    }
}
