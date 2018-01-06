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
package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGObject;
import org.jkiss.dbeaver.debug.DBGProcedureController;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.debug.DBGSessionManager;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugSessionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

public class PostgreProcedureController extends DBGProcedureController<Integer, Integer> {

    public PostgreProcedureController() {
        super();
    }

    @Override
    protected DBGSessionManager<Integer, Integer> initSessionManager(DBCSession session) throws DBGException {
        if (session instanceof JDBCSession) {
            JDBCSession jdbcSession = (JDBCSession) session;
            Connection original;
            try {
                original = jdbcSession.getOriginal();
                return new PostgreDebugSessionManager(original);
            } catch (SQLException e) {
                throw new DBGException("Unable to obtain connection");
            }
        }
        throw new DBGException("Invalid JDBC session handle");
    }
    
    @Override
    protected DBGSession<? extends DBGSessionInfo<Integer>, ? extends DBGObject<Integer>, Integer, Integer> createSession(
            DBCSession session, DBGSessionManager<Integer, Integer> sessionManager) throws DBGException {
        
        if (session instanceof JDBCSession) {
            JDBCSession jdbcSession = (JDBCSession) session;
            Connection original;
            try {
                original = jdbcSession.getOriginal();
                DBGSession<? extends DBGSessionInfo<Integer>, ? extends DBGObject<Integer>, Integer, Integer> created = sessionManager.createDebugSession(original);
                return created;
            } catch (SQLException e) {
                throw new DBGException("Unable to obtain connection");
            }
        }
        throw new DBGException("Invalid JDBC session handle");
    }

}
