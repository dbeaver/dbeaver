/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;

import java.util.*;

/**
 * Query manager execution handler implementation
 */
public class QMMetaCollector extends DefaultExecutionHandler {

    static final Log log = LogFactory.getLog(QMMetaCollector.class);

    private Map<String, QMMSessionInfo> sessionMap = new HashMap<String, QMMSessionInfo>();

    public QMMetaCollector()
    {
    }

    public void dispose()
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
    }

    public String getHandlerName()
    {
        return "Meta info collector";
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
    }

    @Override
    public void handleSessionEnd(DBPDataSource dataSource)
    {
        QMMSessionInfo session = getSession(dataSource);
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit)
    {
        QMMSessionInfo session = getSession(context.getDataSource());
        if (session != null) {
            session.changeTransactional(!autoCommit);
        }
        // Fire transactional mode change
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
    }

    @Override
    public void handleTransactionCommit(DBCExecutionContext context)
    {
        QMMSessionInfo session = getSession(context.getDataSource());
        if (session != null) {
            session.commit();
        }
    }

    @Override
    public void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {
        QMMSessionInfo session = getSession(context.getDataSource());
        if (session != null) {
            session.rollback(savepoint);
        }
    }

    @Override
    public void handleStatementOpen(DBCStatement statement)
    {
        QMMSessionInfo session = getSession(statement.getContext().getDataSource());
        if (session != null) {
            session.openStatement(statement);
        }
    }

    @Override
    public void handleStatementClose(DBCStatement statement)
    {
        QMMSessionInfo session = getSession(statement.getContext().getDataSource());
        if (session != null) {
            if (!session.closeStatement(statement)) {
                log.warn("Could not properly handle statement close");
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
                stat.beginExecution(statement);
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
                stat.endExecution(rows, error);
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
                stat.beginFetch(resultSet);
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
                stat.endFetch(rowCount);
            }
        }
    }
}
