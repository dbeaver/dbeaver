/**
 * 
 */
package org.jkiss.dbeaver.ext.exasol.model.app;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

/**
 * @author Karl Griesser
 *
 */
public class ExasolServerSessionManager implements DBAServerSessionManager<ExasolServerSession> {
	
    public static final String PROP_KILL_QUERY = "killQuery";
	private static final String KILL_APP_CMD = "kill session %d";
	private static final String KILL_STMT_CMD = "kill statement in session %d";
	
	private final ExasolDataSource dataSource;

	/**
	 * 
	 */
	public ExasolServerSessionManager(ExasolDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public DBPDataSource getDataSource() {
		return this.dataSource;
	}

	@Override
	public Collection<ExasolServerSession> getSessions(DBCSession session, Map<String, Object> options)
			throws DBException {
		try {
			return ExasolUtils.readSessions(session.getProgressMonitor(), (JDBCSession) session);
		} catch (SQLException e) {
			throw new DBException(e, session.getDataSource());
		}
	}

	@Override
	public void alterSession(DBCSession session, ExasolServerSession sessionType, Map<String, Object> options)
			throws DBException {
		try {
			String cmd = String.format(Boolean.TRUE.equals(options.get(PROP_KILL_QUERY)) ?  KILL_STMT_CMD : KILL_APP_CMD, sessionType.getSessionID().toString());
			PreparedStatement dbStat = ((JDBCSession) session).prepareStatement(cmd);
			dbStat.execute();
			
			
		} catch (SQLException e) {
			throw new DBException(e, session.getDataSource());
		}

	}
	

}
