/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.QMMetaListener;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;

import java.util.*;

/**
 * Query manager execution handler implementation
 */
public class QMMCollector extends DefaultExecutionHandler {

    static final Log log = LogFactory.getLog(QMMCollector.class);

    private static final long EVENT_DISPATCH_PERIOD = 250;

    private Map<String, QMMSessionInfo> sessionMap = new HashMap<String, QMMSessionInfo>();
    private List<QMMetaListener> listeners = new ArrayList<QMMetaListener>();
    private List<QMMetaEvent> eventPool = new ArrayList<QMMetaEvent>();
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
        return new ArrayList<QMMetaListener>(listeners);
    }

    private synchronized void fireMetaEvent(final QMMObject object, final QMMetaEvent.Action action)
    {
        eventPool.add(new QMMetaEvent(object, action));
    }

    private synchronized List<QMMetaEvent> obtainEvents()
    {
        List<QMMetaEvent> events = eventPool;
        eventPool = new ArrayList<QMMetaEvent>();
        return events;
    }

    private QMMSessionInfo getSession(DBPDataSource dataSource)
    {
        QMMSessionInfo session = sessionMap.get(dataSource.getContainer().getId());
        if (session == null) {
            log.warn("Could not find session meta information: " + dataSource.getContainer().getId());
        }
        return session;
    }

    @Override
    public void handleSessionStart(DBPDataSource dataSource, boolean transactional)
    {
        String containerId = dataSource.getContainer().getId();
        QMMSessionInfo session = new QMMSessionInfo(
            dataSource,
            transactional,
            sessionMap.get(containerId));
        sessionMap.put(containerId, session);

        if (session.getPrevious() != null && !session.getPrevious().isClosed()) {
            log.warn("Previous '" + containerId + "' session wasn't closed");
            session.getPrevious().close();
        }
        fireMetaEvent(session, QMMetaEvent.Action.BEGIN);
    }

    @Override
    public void handleSessionEnd(DBPDataSource dataSource)
    {
        QMMSessionInfo session = getSession(dataSource);
        if (session != null) {
            session.close();
            fireMetaEvent(session, QMMetaEvent.Action.END);
        }
    }

    @Override
    public void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit)
    {
        QMMSessionInfo session = getSession(context.getDataSource());
        if (session != null) {
            QMMTransactionInfo oldTxn = session.changeTransactional(!autoCommit);
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
            fireMetaEvent(session, QMMetaEvent.Action.UPDATE);
        }
        // Fire transactional mode change
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
    }

    @Override
    public void handleTransactionCommit(DBCExecutionContext context)
    {
        QMMSessionInfo session = getSession(context.getDataSource());
        if (session != null) {
            QMMTransactionInfo oldTxn = session.commit();
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {
        QMMSessionInfo session = getSession(context.getDataSource());
        if (session != null) {
            QMMObject oldTxn = session.rollback(savepoint);
            if (oldTxn != null) {
                fireMetaEvent(oldTxn, QMMetaEvent.Action.END);
            }
        }
    }

    @Override
    public void handleStatementOpen(DBCStatement statement)
    {
        QMMSessionInfo session = getSession(statement.getContext().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.openStatement(statement);
            fireMetaEvent(stat, QMMetaEvent.Action.BEGIN);
        }
    }

    @Override
    public void handleStatementClose(DBCStatement statement)
    {
        QMMSessionInfo session = getSession(statement.getContext().getDataSource());
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
    public void handleStatementExecuteBegin(DBCStatement statement)
    {
        QMMSessionInfo session = getSession(statement.getContext().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.getStatement(statement);
            if (stat != null) {
                QMMStatementExecuteInfo exec = stat.beginExecution(statement);
                fireMetaEvent(exec, QMMetaEvent.Action.BEGIN);
            }
        }
    }

    @Override
    public void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error)
    {
        QMMSessionInfo session = getSession(statement.getContext().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.getStatement(statement);
            if (stat != null) {
                QMMStatementExecuteInfo exec = stat.endExecution(rows, error);
                if (exec != null) {
                    fireMetaEvent(exec, QMMetaEvent.Action.END);
                }
            }
        }
    }

    @Override
    public void handleResultSetOpen(DBCResultSet resultSet)
    {
        QMMSessionInfo session = getSession(resultSet.getContext().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.getStatement(resultSet.getSource());
            if (stat != null) {
                QMMStatementExecuteInfo exec = stat.beginFetch(resultSet);
                if (exec != null) {
                    fireMetaEvent(exec, QMMetaEvent.Action.UPDATE);
                }
            }
        }
    }

    @Override
    public void handleResultSetClose(DBCResultSet resultSet, long rowCount)
    {
        QMMSessionInfo session = getSession(resultSet.getContext().getDataSource());
        if (session != null) {
            QMMStatementInfo stat = session.getStatement(resultSet.getSource());
            if (stat != null) {
                QMMStatementExecuteInfo exec = stat.endFetch(rowCount);
                if (exec != null) {
                    fireMetaEvent(exec, QMMetaEvent.Action.UPDATE);
                }
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
            List<QMMetaEvent> events = obtainEvents();
            List<QMMetaListener> listeners = getListeners();
            if (!listeners.isEmpty() && !events.isEmpty()) {
                // Dispatch all events
                for (QMMetaEvent event : events) {
                    for (QMMetaListener listener : listeners) {
                        try {
                            listener.metaInfoChanged(event);
                        } catch (Throwable e) {
                            log.error("Error notifying event listener", e);
                        }
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
