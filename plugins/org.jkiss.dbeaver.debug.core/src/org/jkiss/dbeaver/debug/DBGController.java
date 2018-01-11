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
import java.util.Map;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * This interface is expected to be used in synch manner
 */
public interface DBGController {

    public static final String DATABASE_NAME = "databaseName"; //$NON-NLS-1$
    public static final String PROCEDURE_OID = "procedureOID"; //$NON-NLS-1$
    public static final String PROCESS_ID = "processID"; //$NON-NLS-1$

    /*
     * General lifecycle
     */

    /**
     * Sets debug context like OID etc.
     * @param context
     */
    void init(Map<String, Object> context);

    /**
     * 
     * @param monitor
     * @return key to use for <code>detach</code>
     * @throws DBGException
     */
    Object attach(DBRProgressMonitor monitor) throws DBGException;
    
    void suspend(DBRProgressMonitor monitor) throws DBGException;
    
    void resume(DBRProgressMonitor monitor) throws DBGException;
    
    /**
     * 
     * @param key the key obtained as a result of <code>attach</code>
     * @param monitor
     * @throws DBGException
     */
    void detach(Object key, DBRProgressMonitor monitor) throws DBGException;
    
    void dispose() throws DBGException;
    
    List<? extends DBGStackFrame> getStack(Object id) throws DBGException;

    DBGSessionInfo getSessionInfo(DBCExecutionContext connection) throws DBGException;

    List<? extends DBGSessionInfo> getSessions() throws DBGException;

    DBGSession getDebugSession(Object id) throws DBGException;

    List<DBGSession> getDebugSessions() throws DBGException;

    DBGSession createDebugSession(DBGSessionInfo targetInfo, DBCExecutionContext connection) throws DBGException;

    boolean isSessionExists(Object id);

    List<? extends DBGObject> getObjects(String ownerCtx, String nameCtx) throws DBGException;

}
