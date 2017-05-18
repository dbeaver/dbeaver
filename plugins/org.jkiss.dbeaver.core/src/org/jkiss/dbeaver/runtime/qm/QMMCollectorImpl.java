/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.qm;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMMCollector;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMMetaListener;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.LongKeyMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Query manager execution handler implementation
 */
public class QMMCollectorImpl extends DefaultExecutionHandler implements QMMCollector {

    private static final Log log = Log.getLog(QMMCollectorImpl.class);

    private static final long EVENT_DISPATCH_PERIOD = 250;
    private static final int MAX_HISTORY_EVENTS = 1000;

    // Session map
    private LongKeyMap<QMMSessionInfo> sessionMap = new LongKeyMap<>();
    private List<Long> closedSessions = new ArrayList<>();

    // External listeners
    private List<QMMetaListener> listeners = new ArrayList<>();

    // Temporary event pool
    private List<QMMetaEvent> eventPool = new ArrayList<>();
    // Sync object
    private final Object historySync = new Object();
    // History (may be purged when limit reached)
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

    @NotNull
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
        QMMSessionInfo sessionInfo = sessionMap.get(context.getContextId());
        if (sessionInfo == null) {
            log.debug("Can't find sessionInfo meta information: " + context.getContextId() + " (" + context.getContextName() + ")");
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
    public synchronized void handleContextOpen(@NotNull DBCExecutionContext context, boolean transactional)
    {
        final long contextId = context.getContextId();
        QMMSessionInfo session = sessionMap.get(contextId);
        if (session == null) {
            session = new QMMSessionInfo(
                context,
                transactional);
            sessionMap.put(contextId, session);
        } else {
            // This session may already be in cache in case of reconnect/invalidate
            // (when context closed and reopened without new context object creation)
            session.reopen();
        }

        // Remove from closed sessions (in case of re-opened connection)
        closedSessions.remove(contextId);
        // Notify
        fireMetaEvent(session, QMMetaEvent.Action.BEGIN);
    }

    @Override
    public synchronized void handleContextClose(@NotNull DBCExecutionContext context)
    {
        QMMSessionInfo session = getSessionInfo(context);
        if (session != null) {
            session.close();
            fireMetaEvent(session, QMMetaEvent.Action.END);
        }
        closedSessions.add(context.getContextId());
    }

    @Override
    public synchronized void handleTransactionAutocommit(@NotNull DBCExecutionContext context, boolean autoCommit)
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
    public synchronized void handleTransactionCommit(@NotNull DBCExecutionContext context)
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
    public synchronized void handleTransactionRollback(@NotNull DBCExecutionContext context, DBCSavepoint savepoint)
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
    public synchronized void handleStatementOpen(@NotNull DBCStatement statement)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getExecutionContext());
        if (session != null) {
            QMMStatementInfo stat = session.openStatement(statement);
            fireMetaEvent(stat, QMMetaEvent.Action.BEGIN);
        }
    }

    @Override
    public synchronized void handleStatementClose(@NotNull DBCStatement statement, long rows)
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
    public synchronized void handleStatementExecuteBegin(@NotNull DBCStatement statement)
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
    public synchronized void handleStatementExecuteEnd(@NotNull DBCStatement statement, long rows, Throwable error)
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
    public synchronized void handleResultSetOpen(@NotNull DBCResultSet resultSet)
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
    public synchronized void handleResultSetClose(@NotNull DBCResultSet resultSet, long rowCount)
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
            final List<QMMetaEvent> events;
            List<Long> sessionsToClose;
            synchronized (QMMCollectorImpl.this) {
                events = obtainEvents();
                sessionsToClose = closedSessions;
                closedSessions.clear();
            }
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
            // Cleanup closed sessions
            synchronized (QMMCollectorImpl.this) {
                for (Long sessionId : sessionsToClose) {
                    final QMMSessionInfo session = sessionMap.get(sessionId);
                    if (session != null && !session.isClosed()) {
                        // It is possible (rarely) that session was reopened before event dispatcher run
                        // In that case just ignore it
                        sessionMap.remove(sessionId);
                    }
                }
            }
            if (isRunning()) {
                this.schedule(EVENT_DISPATCH_PERIOD);
            }
            return Status.OK_STATUS;
        }
    }

}
