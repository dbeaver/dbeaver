/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.server;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dpi.api.DPIController;
import org.jkiss.dbeaver.model.dpi.api.DPISession;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DPIControllerImpl implements DPIController {

    private static final Log log = Log.getLog(DPIControllerImpl.class);

    private final DPIRestServer server;
    private final Map<String, DPISession> sessions = new LinkedHashMap<>();

    public DPIControllerImpl(DPIRestServer server) {
        this.server = server;
    }

    @Override
    public String ping() throws DBException {
        return "pong";
    }

    @Override
    public DPISession openSession(String projectId) {
        DPISession session = new DPISession(UUID.randomUUID().toString());
        this.sessions.put(session.getSessionId(), session);
        return session;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull String session, @NotNull String container) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void closeSession(@NotNull String sessionId) throws DBException {
        DPISession session = sessions.remove(sessionId);
        if (session == null) {
            throw new DBException("Session '" + sessionId + "' not found");
        }
        if (sessions.isEmpty()) {
            server.stopServer();
        }
    }

    @Override
    public void close() {

    }
}
