/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model.session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oceanbase.oracle.model.OceanbaseOracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.session.OracleServerSession;
import org.jkiss.dbeaver.ext.oracle.model.session.OracleServerSessionManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetails;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetailsProvider;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManagerSQL;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.utils.CommonUtils;

public class OceanbaseOracleServerSessionManager implements DBAServerSessionManager<OceanbaseOracleServerSession>, DBAServerSessionManagerSQL, DBAServerSessionDetailsProvider{
	private final OceanbaseOracleDataSource dataSource;
	
	public OceanbaseOracleServerSessionManager (OceanbaseOracleDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
    public Collection<OceanbaseOracleServerSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException {
        try {

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(generateSessionReadQuery(options))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<OceanbaseOracleServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new OceanbaseOracleServerSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }
	
	@Override
    public String generateSessionReadQuery(Map<String, Object> options) {
        return "SHOW FULL PROCESSLIST";
    }

	@Override
	public List<DBAServerSessionDetails> getSessionDetails() {
		return null;
	}

	@Override
	public boolean canGenerateSessionReadQuery() {
		return true;
	}

	@Override
	public DBPDataSource getDataSource() {
		return this.dataSource;
	}

	@Override
	public void alterSession(DBCSession session, OceanbaseOracleServerSession sessionType, Map<String, Object> options)
			throws DBException {
		final boolean toKill = Boolean.TRUE.equals(options.get("killSession"));
        final boolean immediate = Boolean.TRUE.equals(options.get("immediate"));

        try {
            StringBuilder sql = new StringBuilder("KILL ");
            sql.append(sessionType.getSid());
            if (immediate) {
                sql.append(" IMMEDIATE");
            }
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
	}



}
