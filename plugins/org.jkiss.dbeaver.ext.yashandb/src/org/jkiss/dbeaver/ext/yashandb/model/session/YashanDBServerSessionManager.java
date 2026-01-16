/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
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

public class YashanDBServerSessionManager implements DBAServerSessionManager<YashanDBServerSession>,
		DBAServerSessionManagerSQL, DBAServerSessionDetailsProvider {

	public YashanDBServerSessionManager(YashanDBDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static final String PROP_KILL_SESSION = "killSession";

	public static final String OPTION_SHOW_BACKGROUND = "showBackground";
	public static final String OPTION_SHOW_INACTIVE = "showInactive";

	private final YashanDBDataSource dataSource;

	@Override
	public DBPDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public Collection<YashanDBServerSession> getSessions(DBCSession session, Map<String, Object> options)
			throws DBException {
		try {

			try (JDBCPreparedStatement dbStat = ((JDBCSession) session)
					.prepareStatement(generateSessionReadQuery(options))) {
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					List<YashanDBServerSession> sessions = new ArrayList<>();
					while (dbResult.next()) {
						sessions.add(new YashanDBServerSession(dbResult));
					}
					return sessions;
				}
			}
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	}

	@Override
	public boolean canGenerateSessionReadQuery() {
		return true;
	}

	@Override
	public List<DBAServerSessionDetails> getSessionDetails() {
		return null;
	}

	@Override
	public void alterSession(DBCSession session, String sessionId, Map<String, Object> options) throws DBException {
		final boolean toKill = Boolean.TRUE.equals(options.get(PROP_KILL_SESSION));
		if (toKill) {
			try {
				String sql = "ALTER SYSTEM KILL SESSION " + sessionId;
				try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
					dbStat.execute();
				}
			} catch (SQLException e) {
				throw new DBException(e.getMessage());
			}
		}
	}

	@Override
	public Map<String, Object> getTerminateOptions() {
		return Map.of(YashanDBServerSessionManager.PROP_KILL_SESSION, true);
	}

	@Override
	public String generateSessionReadQuery(Map<String, Object> options) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT s.*, sq.SQL_TEXT SQL_TEXT,sq.CHILD_NUMBER SQL_CHILD_NUMBER\n"
				+ "FROM (SELECT * FROM V$SESSION s WHERE 1=1 ");
		if (!CommonUtils.getOption(options, OPTION_SHOW_BACKGROUND)) {
			sql.append(" AND s.TYPE = 'USER'");
		}
		if (!CommonUtils.getOption(options, OPTION_SHOW_INACTIVE)) {
			sql.append(" AND s.STATUS <> 'INACTIVE'");
		}
		sql.append(") s LEFT join\n (SELECT * FROM v$sql WHERE PARSING_SCHEMA_NAME='SYS') sq \n"
				+ " on s.sql_hash_value = sq.hash_value");
		return sql.toString();
	}
}
