/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.internal.debug.core.model;

import java.sql.SQLException;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugController;
import org.jkiss.dbeaver.ext.postgresql.internal.debug.core.PostgreSqlDebugCoreMessages;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;

public class PgSqlDebugController extends DatabaseDebugController {
    
    private static final String PROPERTY_APPLICATION_NAME = "ApplicationName"; //$NON-NLS-1$
    private static final String SELECT_FROM_PLDBG_ABORT_TARGET = "select * from pldbg_abort_target(?)"; //$NON-NLS-1$
    private static final String SELECT_PLDBG_CREATE_LISTENER = "select pldbg_create_listener() as sessionid"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PgSqlDebugController.class);

    private Integer sessionId;
    
    public PgSqlDebugController(String datasourceId, String databaseName, Map<String, Object> attributes)
    {
        super(datasourceId, databaseName, attributes);
    }
    
    public Integer getSessionId()
    {
        return sessionId;
    }
    
    @Override
    protected void afterSessionOpen(DBCSession session)
    {
        super.afterSessionOpen(session);
        if (session instanceof JDBCSession ) {
            JDBCSession jdbcSession = (JDBCSession) session;
            String query = SELECT_PLDBG_CREATE_LISTENER;
            try (final JDBCStatement jdbcStat = jdbcSession.prepareStatement(DBCStatementType.QUERY, query, false, false, false)) {
                if (jdbcStat.executeStatement()) {
                    try (final DBCResultSet dbResult = jdbcStat.openResultSet()) {
                        while (dbResult.nextRow()) {
                            final Object cellValue = dbResult.getAttributeValue(0);
                            if (cellValue instanceof Integer) {
                                this.sessionId = (Integer) cellValue;
                                String applicationName = NLS.bind(PostgreSqlDebugCoreMessages.PgSqlDebugController_connection_application_name, sessionId);
                                try {
                                    jdbcSession.getOriginal().setClientInfo(PROPERTY_APPLICATION_NAME, applicationName);
                                } catch (SQLException e) {
                                    throw new DBCException(e, session.getDataSource());
                                }
                            }
                        }
                    }
                }
            } catch (DBCException e) {
                String message = NLS.bind(PostgreSqlDebugCoreMessages.PgSqlDebugController_e_failed_session_open, session.getDataSource());
                log.error(message, e);
            }
        }
    }
    
    @Override
    protected void beforeSessionClose(DBCSession session)
    {
        if (session instanceof JDBCSession) {
            JDBCSession jdbcSession = (JDBCSession) session;
            String query = SELECT_FROM_PLDBG_ABORT_TARGET;
            try (final JDBCPreparedStatement prepared = jdbcSession.prepareStatement(query)) {
                prepared.setInt(1, sessionId);
                prepared.execute();
            } catch (SQLException e) {
                String message = NLS.bind(PostgreSqlDebugCoreMessages.PgSqlDebugController_e_failed_session_close, sessionId, session.getDataSource());
                log.error(message, e);
            }
        }
        super.beforeSessionClose(session);
    }

}
