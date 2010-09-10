/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Query manager execution handler implementation
 */
public class QMExecutionHandlerImpl implements QMExecutionHandler, DBPEventListener {

    private QMControllerImpl controller;
    private Map<DBSDataSourceContainer, QMDataSourceMetaInfo> dataSourcesInfo = new HashMap<DBSDataSourceContainer, QMDataSourceMetaInfo>();

    public QMExecutionHandlerImpl(QMControllerImpl controller)
    {
    	this.controller = controller;
    	this.controller.getDataSourceRegistry().addDataSourceListener(this);
    }

    public String getHandlerName() {
        return "Default";
    }

    public void handleTransactionCommit(DBCExecutionContext context)
    {
        // Notify other handlers
        for (QMExecutionHandler handler : controller.getHandlers()) {
            handler.handleTransactionCommit(context);
        }
    }

    public void handleTransactionSavepoint(DBCSavepoint savepoint)
    {
        
    }

    public void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {
        
    }

    public void handleStatementOpen(DBCStatement statement)
    {
        
    }

    public void handleStatementExecute(DBCStatement statement)
    {
        
    }

    public void handleStatementBind(DBCStatement statement, Object column, Object value)
    {
        
    }

    public void handleStatementClose(DBCStatement statement)
    {
        
    }

    public void handleResultSetOpen(DBCResultSet resultSet)
    {
        
    }

    public void handleResultSetFetch(DBCResultSet resultSet)
    {
        
    }

    public void handleResultSetClose(DBCResultSet resultSet)
    {
        
    }

    public void handleDataSourceEvent(DBPEvent event)
    {
        if (event.getObject() instanceof DBSDataSourceContainer && event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getEnabled() != null) {
            if (event.getEnabled()) {
                dataSourcesInfo.put((DBSDataSourceContainer)event.getObject(), new QMDataSourceMetaInfo());
            } else {
                dataSourcesInfo.remove((DBSDataSourceContainer)event.getObject());
            }
        }
    }

    public void dispose()
    {
        if (controller != null) {
            controller.getDataSourceRegistry().removeDataSourceListener(this);
            controller = null;
        }
    }

}
