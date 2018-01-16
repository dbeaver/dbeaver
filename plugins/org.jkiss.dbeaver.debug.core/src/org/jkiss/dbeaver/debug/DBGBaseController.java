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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.internal.DebugMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public abstract class DBGBaseController implements DBGController {
    
    private static final Log log = Log.getLog(DBGBaseController.class);

    private final DBPDataSourceContainer dataSourceContainer;

    private final Map<String, Object> configuration = new HashMap<String, Object>();
    private final Map<Object, DBGSession> sessions = new HashMap<Object, DBGSession>(1);

    private ListenerList<DBGEventHandler> eventHandlers = new ListenerList<>();

    private DBCExecutionContext executionContext;

    public DBGBaseController(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }
    
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }
    
    @Override
    public void init(Map<String, Object> context) {
        this.configuration.putAll(context);
    }
    
    @Override
    public Object attach(DBRProgressMonitor monitor) throws DBGException {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (!dataSourceContainer.isConnected()) {
            throw new DBGException("Not connected to database");
        }
        try {
            this.executionContext = dataSource.openIsolatedContext(monitor, "Debug controller");
            DBGSessionInfo targetInfo = getSessionDescriptor(getExecutionContext());
            DBCExecutionContext sessionContext = dataSource.openIsolatedContext(monitor, "Debug session");
            DBGSession debugSession = createSession(targetInfo, sessionContext);
            Object id = targetInfo.getID();
            sessions.put(id, debugSession);
            attachSession(debugSession, sessionContext, configuration, monitor);
            return id;
        } catch (DBException e) {
            String message = NLS.bind(DebugMessages.DatabaseDebugController_e_opening_debug_context,
                dataSourceContainer);
            log.error(message, e);
            throw new DBGException(message, e);
        }
    }
    
    public abstract void attachSession(DBGSession session, DBCExecutionContext sessionContext, Map<String, Object> configuataion, DBRProgressMonitor monitor) throws DBGException, DBException;

    @Override
    public void resume(DBRProgressMonitor monitor) throws DBGException {
        // TODO Auto-generated method stub

    }

    @Override
    public void suspend(DBRProgressMonitor monitor) throws DBGException {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void detach(Object sessionkey, DBRProgressMonitor monitor) throws DBGException {
        DBGSession session = sessions.remove(sessionkey);
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void dispose() {
        executionContext.close();
        Collection<DBGSession> values = sessions.values();
        for (DBGSession session : values) {
            session.close();
        }
        Object[] listeners = eventHandlers.getListeners();
        for (Object listener : listeners) {
            unregisterEventHandler((DBGEventHandler) listener);
        }
    }
    
    @Override
    public List<? extends DBGStackFrame> getStack(Object id) throws DBGException {
        DBGSession session = findSession(id);
        if (session == null) {
            String message = NLS.bind("Session for {0} is not available", id);
            throw new DBGException(message);
        }
        return session.getStack();
    }

    @Override
    public DBGSession findSession(Object id) throws DBGException {
        return sessions.get(id);
    }

    @Override
    public boolean isSessionExists(Object id) {
        return sessions.containsKey(id);
    }

    @Override
    public List<DBGSession> getSessions() throws DBGException {
        return new ArrayList<DBGSession>(sessions.values());
    }
    
    @Override
    public boolean canStepInto(Object sessionKey) {
        return true;
    }

    @Override
    public boolean canStepOver(Object sessionKey) {
        return true;
    }

    @Override
    public boolean canStepReturn(Object sessionKey) {
        // hmm, not sure 
        return false;
    }

    @Override
    public void stepInto(Object sessionKey) throws DBGException {
        DBGSession session = findSession(sessionKey);
        if (session == null) {
            String message = NLS.bind("Session for {0} is not available", sessionKey);
            throw new DBGException(message);
        }
        session.execStepInto();
    }
    
    @Override
    public void stepOver(Object sessionKey) throws DBGException {
        DBGSession session = findSession(sessionKey);
        if (session == null) {
            String message = NLS.bind("Session for {0} is not available", sessionKey);
            throw new DBGException(message);
        }
        session.execStepOver();
    }

    @Override
    public void stepReturn(Object sessionKey) throws DBGException {
        //throw DBGException?
    }
    
    @Override
    public void registerEventHandler(DBGEventHandler eventHandler) {
        eventHandlers.add(eventHandler);
    }
    
    @Override
    public void unregisterEventHandler(DBGEventHandler eventHandler) {
        eventHandlers.remove(eventHandler);
    }
    
    public void fireEvent(DBGEvent event) {
        for (DBGEventHandler eventHandler : eventHandlers) {
            eventHandler.handleDebugEvent(event);
        }
    }
    
}
