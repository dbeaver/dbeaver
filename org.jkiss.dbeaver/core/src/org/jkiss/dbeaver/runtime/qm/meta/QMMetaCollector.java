/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
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

    @Override
    public void handleSessionStart(DBPDataSource dataSource)
    {
        String containerId = dataSource.getContainer().getId();
        QMMSessionInfo session = new QMMSessionInfo(
            dataSource,
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
        QMMSessionInfo session = sessionMap.get(dataSource.getContainer().getId());
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit)
    {
        // Fire transactional mode change
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
    }

    @Override
    public void handleStatementExecuteBegin(DBCStatement statement)
    {

    }

}
