/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.QMMetaListener;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;

import java.util.*;

/**
 * Query manager execution handler implementation
 */
public class QMMCollector extends DefaultExecutionHandler {

    static final Log log = Log.getLog(QMMCollector.class);

    private static final long EVENT_DISPATCH_PERIOD = 250;

    private Map<String, QMMSessionInfo> sessionMap = new HashMap<String, QMMSessionInfo>();
    private List<QMMetaListener> listeners = new ArrayList<QMMetaListener>();
    private List<QMMetaEvent> eventPool = new ArrayList<QMMetaEvent>();
    private final List<QMMetaEvent> pastEvents = new ArrayList<QMMetaEvent>();
    private boolean running = true;

    public QMMCollector()
    {
        new EventDispatcher().schedule(EVENT_DISPATCH_PERIOD);
    }

    public synchronized void dispose()
    {
        if (!sessionMap.isEmpty()) {
            List<QMMSessionInfo> openSessions = new ArrayList<QMMSessionInfo>();
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
        return new ArrayList<QMMetaListener>(listeners);
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
        eventPool = new ArrayList<QMMetaEvent>();
        return events;
    }

    public QMMSessionInfo getSessionInfo(DBCExecutionContext context)
    {
        String contextId = context.getDataSource().getContainer().getId() + ":" + context.getContextName();
        QMMSessionInfo sessionInfo = sessionMap.get(contextId);
        if (sessionInfo == null) {
            log.warn("Could not find sessionInfo meta information: " + contextId);
        }
        return sessionInfo;
    }

    public List<QMMetaEvent> getPastEvents()
    {
        synchronized (pastEvents) {
            return new ArrayList<QMMetaEvent>(pastEvents);
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
        QMMSessionInfo sessionInfo = getSessionInfo(context.getDataSource());
        if (sessionInfo != null) {
            QMMTransactionInfo oldTxn = sessionInfo.changeTransactional(!autoCommit);
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
            fireMetaEvent(sessionInfo, QMMetaEvent.Action.UPDATE);
        }
        // Fire transactional mode change
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
        DataSourcePropertyTester.fireCommandRefresh(ICommandIds.CMD_TOGGLE_AUTOCOMMIT);
    }

    @Override
    public synchronized void handleTransactionCommit(DBCExecutionContext context)
    {
        QMMSessionInfo sessionInfo = getSessionInfo(context.getDataSource());
        if (sessionInfo != null) {
            QMMTransactionInfo oldTxn = sessionInfo.commit();
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
        }
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
    }

    @Override
    public synchronized void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {
        QMMSessionInfo sessionInfo = getSessionInfo(context.getDataSource());
        if (sessionInfo != null) {
            QMMObject oldTxn = sessionInfo.rollback(savepoint);
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
        }
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
    }

    @Override
    public synchronized void handleStatementOpen(DBCStatement statement)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.openStatement(statement);
            fireMetaEvent(stat, QMMetaEvent.Action.BEGIN);
        }
    }

    @Override
    public synchronized void handleStatementClose(DBCStatement statement)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.closeStatement(statement);
            if (stat == null) {
                log.warn("Could not properly handle statement close");
            } else {
                fireMetaEvent(stat, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public synchronized void handleStatementExecuteBegin(DBCStatement statement)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getDataSource());
        if (session != null) {
            QMMStatementExecuteInfo exec = session.beginExecution(statement);
            if (exec != null) {
                fireMetaEvent(exec, QMMetaEvent.Action.BEGIN);
            }
        }
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
    }

    @Override
    public synchronized void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error)
    {
        QMMSessionInfo session = getSessionInfo(statement.getSession().getDataSource());
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
        QMMSessionInfo session = getSessionInfo(resultSet.getSession().getDataSource());
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
        QMMSessionInfo session = getSessionInfo(resultSet.getSession().getDataSource());
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
                UIUtils.runInUI(null, new Runnable() {
                    @Override
                    public void run()
                    {
                        for (QMMetaListener listener : listeners) {
                            try {
                                listener.metaInfoChanged(events);
                            } catch (Throwable e) {
                                log.error("Error notifying event listener", e);
                            }
                        }
                    }
                });
            }
            synchronized (pastEvents) {
                pastEvents.addAll(events);
            }
            if (isRunning()) {
                this.schedule(EVENT_DISPATCH_PERIOD);
            }
            return Status.OK_STATUS;
        }
    }

}
