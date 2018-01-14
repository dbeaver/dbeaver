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

import java.util.List;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

public interface DBGSessionManager {
    DBGSessionInfo getSessionInfo(DBCExecutionContext connection) throws DBGException;

    List<? extends DBGSessionInfo> getSessions() throws DBGException;

    DBGSession getDebugSession(Object id) throws DBGException;

    List<DBGSession> getDebugSessions() throws DBGException;

    void terminateSession(Object id);

    DBGSession createDebugSession(DBCExecutionContext connection) throws DBGException;

    boolean isSessionExists(Object id);

    List<? extends DBGObject> getObjects(String ownerCtx, String nameCtx) throws DBGException;

    void dispose();
}
