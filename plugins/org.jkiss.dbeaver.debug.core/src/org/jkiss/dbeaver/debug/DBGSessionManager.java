/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.debug;

import java.sql.Connection;
import java.util.List;

public interface DBGSessionManager<SESSION_ID_TYPE, OBJECT_ID_TYPE> {
    DBGSessionInfo<SESSION_ID_TYPE> getSessionInfo(Connection connection) throws DBGException;

    List<? extends DBGSessionInfo<SESSION_ID_TYPE>> getSessions() throws DBGException;

    DBGSession<? extends DBGSessionInfo<SESSION_ID_TYPE>, ? extends DBGObject<OBJECT_ID_TYPE>, SESSION_ID_TYPE, OBJECT_ID_TYPE> getDebugSession(SESSION_ID_TYPE id) throws DBGException;

    List<DBGSession<?, ?, SESSION_ID_TYPE, OBJECT_ID_TYPE>> getDebugSessions() throws DBGException;

    void terminateSession(SESSION_ID_TYPE id);

    DBGSession<? extends DBGSessionInfo<SESSION_ID_TYPE>, ? extends DBGObject<OBJECT_ID_TYPE>, SESSION_ID_TYPE, OBJECT_ID_TYPE> createDebugSession(Connection connection) throws DBGException;

    boolean isSessionExists(SESSION_ID_TYPE id);

    List<? extends DBGObject<OBJECT_ID_TYPE>> getObjects(String ownerCtx, String nameCtx) throws DBGException;

    void dispose();
}
