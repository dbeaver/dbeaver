/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.internal.DebugMessages;
import org.jkiss.dbeaver.debug.jdbc.DBGJDBCSession;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.*;

public abstract class DBGBaseController implements DBGController {

    private static final Log log = Log.getLog(DBGBaseController.class);

    private final DBPDataSourceContainer dataSourceContainer;

    private final Map<String, Object> configuration;
    private final Map<Object, DBGJDBCSession> sessions = new HashMap<>(1);

    private ListenerList<DBGEventHandler> eventHandlers = new ListenerList<>();

    protected DBGBaseController(DBPDataSourceContainer dataSourceContainer, Map<String, Object> configuration) {
        this.dataSourceContainer = dataSourceContainer;
        this.configuration = new HashMap<>(configuration);
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @Override
    public Map<String, Object> getDebugConfiguration() {
        return new HashMap<>(configuration);
    }

    @Override
    public Object attach(DBRProgressMonitor monitor) throws DBGException {
        if (!dataSourceContainer.isConnected()) {
            try {
                dataSourceContainer.connect(monitor, true, true);
            } catch (DBException e) {
                throw new DBGException(e, dataSourceContainer.getDataSource());
            }
        }
        if (!dataSourceContainer.isConnected()) {
            throw new DBGException("Not connected to database");
        }
        try {
            DBGJDBCSession debugSession = createSession(monitor, configuration);

            Object targetId = debugSession.getSessionInfo().getID();

            sessions.put(targetId, debugSession);

            return targetId;
        } catch (DBException e) {
            String message = NLS.bind(DebugMessages.DatabaseDebugController_e_opening_debug_context,
                    dataSourceContainer);
            log.error(message, e);
            throw new DBGException(message, e);
        }
    }

    @Override
    public boolean canSuspend(Object sessionKey) {
        return false;
    }

    @Override
    public boolean canResume(Object sessionKey) {
        return isSessionAccessible(sessionKey);
    }

    @Override
    public void suspend(Object sessionkey) throws DBGException {
        // not supported by default
    }

    @Override
    public void resume(Object sessionKey) throws DBGException {
        DBGSession session = ensureSessionAccessible(sessionKey);
        session.execContinue();
    }

    @Override
    public void detach(DBRProgressMonitor monitor, Object sessionkey) throws DBGException {
        DBGSession session = sessions.remove(sessionkey);
        if (session != null) {
            session.closeSession(monitor);
        }
    }

    @Override
    public void dispose() {
        Collection<DBGJDBCSession> values = sessions.values();
        for (DBGJDBCSession session : values) {
            try {
                session.closeSession(new VoidProgressMonitor());
            } catch (DBGException e) {
                String message = NLS.bind("Error while closing session {0}", session);
                log.error(message, e);
            }
        }
        Object[] listeners = eventHandlers.getListeners();
        for (Object listener : listeners) {
            unregisterEventHandler((DBGEventHandler) listener);
        }
    }

    @Override
    public List<? extends DBGBreakpointDescriptor> getBreakpoints(Object sessionKey) throws DBGException {
        DBGJDBCSession session = ensureSessionAccessible(sessionKey);
        return session.getBreakpoints();
    }

    @Override
    public void addBreakpoint(DBRProgressMonitor monitor, Object sessionKey, DBGBreakpointDescriptor descriptor) throws DBGException {
        DBGJDBCSession session = ensureSessionAccessible(sessionKey);
        session.addBreakpoint(monitor, descriptor);
    }

    @Override
    public void removeBreakpoint(DBRProgressMonitor monitor, Object sessionKey, DBGBreakpointDescriptor descriptor) throws DBGException {
        DBGJDBCSession session = ensureSessionAccessible(sessionKey);
        session.removeBreakpoint(monitor, descriptor);
    }

    @Override
    public List<? extends DBGStackFrame> getStack(Object id) throws DBGException {
        DBGSession session = ensureSessionAccessible(id);
        return session.getStack();
    }

    @Override
    public List<? extends DBGVariable<?>> getVariables(Object id, DBGStackFrame stack) throws DBGException {
        DBGSession session = ensureSessionAccessible(id);
        if (stack != null) {
            session.selectFrame(stack.getLevel());
        }
        return session.getVariables();
    }

    @Override
    public String getSource(Object sessionKey, DBGStackFrame stack) throws DBGException {
        DBGSession session = ensureSessionAccessible(sessionKey);
        return session.getSource(stack);
    }

    public abstract DBGJDBCSession createSession(DBRProgressMonitor monitor, Map<String, Object> configuration)
            throws DBGException;

    protected DBGJDBCSession findSession(Object id) {
        return sessions.get(id);
    }

    public boolean isSessionExists(Object id) {
        return sessions.containsKey(id);
    }

    public List<DBGSession> getSessions() throws DBGException {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public boolean canStepInto(Object sessionKey) {
        return isSessionAccessible(sessionKey);
    }

    @Override
    public boolean canStepOver(Object sessionKey) {
        return isSessionAccessible(sessionKey);
    }

    @Override
    public boolean canStepReturn(Object sessionKey) {
        // hmm, not sure
        return false;
    }

    @Override
    public void stepInto(Object sessionKey) throws DBGException {
        DBGSession session = ensureSessionAccessible(sessionKey);
        session.execStepInto();
    }

    @Override
    public void stepOver(Object sessionKey) throws DBGException {
        DBGSession session = ensureSessionAccessible(sessionKey);
        session.execStepOver();
    }

    @Override
    public void stepReturn(Object sessionKey) throws DBGException {
        // throw DBGException?
    }

    protected DBGJDBCSession ensureSessionAccessible(Object sessionKey) throws DBGException {
        DBGJDBCSession session = findSession(sessionKey);
        if (session == null) {
            String message = NLS.bind("Session for {0} is not available", sessionKey);
            throw new DBGException(message);
        }
        boolean isAccessible = session.isAttached() && !session.isWaiting() && session.isDone();
        if (!isAccessible) {
            String message = NLS.bind("Session for {0} is not accessible", sessionKey);
            throw new DBGException(message);
        }
        return session;
    }

    protected boolean isSessionAccessible(Object sessionKey) {
        DBGJDBCSession session = findSession(sessionKey);
        return session != null && session.isAttached() && !session.isWaiting() && session.isDone();
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

    /*
     * protected void executeProcedure(DBPDataSource dataSource, Map<String,
     * Object> configuration, DBRProgressMonitor monitor) throws DBException {
     * String procedureName = String.valueOf(configuration.get(PROCEDURE_NAME));
     * String call = String.valueOf(configuration.get(PROCEDURE_CALL)); String
     * taskName = NLS.bind("Execute procedure {0}", procedureName); Job job =
     * new Job(taskName) {
     * 
     * @Override protected IStatus run(IProgressMonitor monitor) { try { try
     * (final DBCSession execSession = DBUtils.openUtilSession(new
     * VoidProgressMonitor(), dataSource, taskName)) { try (final DBCStatement
     * dbStat = execSession.prepareStatement(DBCStatementType.EXEC, call, true,
     * false, false)) { dbStat.executeStatement(); } } } catch (DBCException e)
     * { log.error(taskName, e); return DebugCore.newErrorStatus(taskName, e);
     * 
     * } return Status.OK_STATUS; } }; job.schedule(); }
     */

}
