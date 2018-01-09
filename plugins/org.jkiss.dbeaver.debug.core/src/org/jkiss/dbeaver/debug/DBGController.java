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
package org.jkiss.dbeaver.debug;

import java.util.List;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * This interface is expected to be used in synch manner
 */
public interface DBGController {

    DBGSession connect(DBRProgressMonitor monitor) throws DBGException;

    void resume(DBRProgressMonitor monitor, DBGSession session) throws DBGException;

    void suspend(DBRProgressMonitor monitor, DBGSession session) throws DBGException;

    void terminate(DBRProgressMonitor monitor, DBGSession session) throws DBGException;

    void dispose() throws DBGException;

    DBGSessionInfo getSessionInfo(DBCExecutionContext connection) throws DBGException;

    List<? extends DBGSessionInfo> getSessions() throws DBGException;

    DBGSession getDebugSession(Object id) throws DBGException;

    List<DBGSession> getDebugSessions() throws DBGException;

    void terminateSession(Object id);

    DBGSession createDebugSession(DBGSessionInfo targetInfo, DBCExecutionContext connection) throws DBGException;

    boolean isSessionExists(Object id);

    List<? extends DBGObject> getObjects(String ownerCtx, String nameCtx) throws DBGException;

}
