/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.runtime.qm;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMMCollector;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMMetaListener;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;

import java.util.*;

/**
 * Query manager execution handler implementation
 */
public class QMMCollectorImpl extends DefaultExecutionHandler implements QMMCollector {

    private static final Log log = Log.getLog(QMMCollectorImpl.class);

    private static final long EVENT_DISPATCH_PERIOD = 250;
    private static final int MAX_HISTORY_EVENTS = 1000;

    private Map<String, QMMSessionInfo> sessionMap = new HashMap<>();
    private List<QMMetaListener> listeners = new ArrayList<>();
    private List<QMMetaEvent> eventPool = new ArrayList<>();
    private final Object historySync = new Object();
    private List<QMMetaEvent> pastEvents = new ArrayList<>();
    private boolean running = true;

    public QMMCollectorImpl()
    {
        new EventDispatcher().schedule(EVENT_DISPATCH_PERIOD);
    }

    public synchronized void dispose()
    {
        if (!sessionMap.isEmpty()) {
            List<QMMSessionInfo> openSessions = new ArrayList<>();
            for (QMMSessionInfo session : sessionMap.values()) {
                if (!session.isClosed()) {
                    openSessions.add(session);
                }
            }
            if (!openSessions.isEmpty()) {
                log.warn("Some sessions are still open: " + openSessions);
            }
        }
        if (!listeners.isEmpty()) {
            log.warn("Some QM meta collector listeners are still open: " + listeners);
            listeners.clear();
        }
        running = false;
    }

    boolean isRunning()
    {
        return running;
    }

    @Override
    public String getHandlerName()
    {
        return "Meta info collector";
    }

    public synchronized void addListener(QMMetaListener listener)
    {
        listeners.add(listener);
    }

    public synchronized void removeListener(QMMetaListener listener)
    {
        if (!listeners.remove(listener)) {
            log.warn("Listener '" + listener + "' is not registered in QM meta collector");
        }
    }

    private synchronized List<QMMetaListener> getListeners()
    {
        if (listeners.isEmpty()) {
            return Collections.emptyList();
        }
        if (listeners.size() == 1) {
            return Collections.singletonList(listeners.get(0));
        }
        return new ArrayList<>(listeners);
    }

    private synchronized void fireMetaEvent(final QMMObject object, final QMMetaEvent.Action action)
    {
        eventPool.add(new QMMetaEvent(object, action));
    }

    private synchronized List<QMMetaEvent> obtainEvents()
    {
        if (eventPool.isEmpty()) {
            return Collections.emptyList();
        }
        List<QMMetaEvent> events = eventPool;
        eventPool = new ArrayList<>();
        return events;
    }

    public QMMSessionInfo getSessionInfo(DBCExecutionContext context)
    {
        String contextId = context.getDataSource().getContainer().getId() + ":" + context.getContextName();
        QMMSessionInfo sessionInfo = sessionMap.get(contextId);
        if (sessionInfo == null) {
            log.warn("Can't find sessionInfo meta information: " + contextId);
        }
        return sessionInfo;
    }

    public List<QMMetaEvent> getPastEvents()
    {
        synchronized (historySync) {
            return new ArrayList<>(pastEvents);
        }
    }

    @Override
    public synchronized void handleContextOpen(DBCExecutionContext context, boolean transactional)
    {
        String contextId = context.getDataSource().getContainer().getId() + ":" + context.getContextName();
        QMMSessionInfo session = new QMMSessionInfo(
            context,
            transactional,
            sessionMap.get(contextId));
        sessionMap.put(contextId, session);

        if (session.getPrevious() != null && !session.getPrevious().isClosed()) {
            // Is it really a problem? Maybe better to remove warning at all
            // Happens when we have open connection and perform another connection test
            log.debug("Previous '" + contextId + "' session wasn't closed");
        }
        fireMetaEvent(session, QMMetaEvent.Action.BEGIN);
    }

    @Override
    public synchronized void handleContextClose(DBCExecutionContext context)
    {
        QMMSessionInfo session = getSessionInfo(context);
        if (session != null) {
            session.close();
            fireMetaEvent(session, QMMetaEvent.Action.END);
        }
    }

    @Override
    public synchronized void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit)
    {
        QMMSessionInfo sessionInfo = getSessionInfo(context);
        if (sessionInfo != null) {
            QMMTransactionInfo oldTxn = sessionInfo.changeTransactional(!autoCommit);
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
            fireMetaEvent(sessionInfo, QMMetaEvent.Action.UPDATE);
        }
    }

    @Override
    public synchronized void handleTransactionCommit(DBCExecutionContext context)
    {
        QMMSessionInfo sessionInfo = getSessionInfo(context);
        if (sessionInfo != null) {
            QMMTransactionInfo oldTxn = sessionInfo.commit();
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public synchronized void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {
        QMMSessionInfo sessionInfo = getSessionInfo(context);
        if (sessionInfo != null) {
            QMMObject oldTxn = sessionInfo.rollback(savepoint);
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public synchronized void handleStatementOpen(DBCStatement statement)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementInfo stat = session.openStatement(statement);
            fireMetaEvent(stat, QMMetaEvent.Action.BEGIN);
        }
    }

    @Override
    public synchronized void handleStatementClose(DBCStatement statement, long rows)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementInfo stat = session.closeStatement(statement, rows);
            if (stat == null) {
                log.warn("Can't properly handle statement close");
            } else {
                fireMetaEvent(stat, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public synchronized void handleStatementExecuteBegin(DBCStatement statement)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementExecuteInfo exec = session.beginExecution(statement);
            if (exec != null) {
                fireMetaEvent(exec, QMMetaEvent.Action.BEGIN);
            }
        }
    }

    @Override
    public synchronized void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementExecuteInfo exec = session.endExecution(statement, rows, error);
            if (exec != null) {
                fireMetaEvent(exec, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public synchronized void handleResultSetOpen(DBCResultSet resultSet)
    {
        QMMSessionInfo session = getSessionInfo(resultSet.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementExecuteInfo exec = session.beginFetch(resultSet);
            if (exec != null) {
                fireMetaEvent(exec, QMMetaEvent.Action.UPDATE);
            }
        }
    }

    @Override
    public synchronized void handleResultSetClose(DBCResultSet resultSet, long rowCount)
    {
        QMMSessionInfo session = getSessionInfo(resultSet.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementExecuteInfo exec = session.endFetch(resultSet, rowCount);
            if (exec != null) {
                fireMetaEvent(exec, QMMetaEvent.Action.UPDATE);
            }
        }
    }

    private class EventDispatcher extends AbstractJob {

        protected EventDispatcher()
        {
            super("QM meta events dispatcher");
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            final List<QMMetaEvent> events = obtainEvents();
            final List<QMMetaListener> listeners = getListeners();
            if (!listeners.isEmpty() && !events.isEmpty()) {
                // Dispatch all events
                for (QMMetaListener listener : listeners) {
                    try {
                        listener.metaInfoChanged(events);
                    } catch (Throwable e) {
                        log.error("Error notifying event listener", e);
                    }
                }
            }
            synchronized (historySync) {
                pastEvents.addAll(events);
                int size = pastEvents.size();
                if (size > MAX_HISTORY_EVENTS) {
                    pastEvents = new ArrayList<>(pastEvents.subList(
                        size - MAX_HISTORY_EVENTS,
                        size));
                }
            }
            if (isRunning()) {
                this.schedule(EVENT_DISPATCH_PERIOD);
            }
            return Status.OK_STATUS;
        }
    }

}
