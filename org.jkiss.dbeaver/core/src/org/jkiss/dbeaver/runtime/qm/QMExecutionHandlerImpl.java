/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.event.IDataSourceListener;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;

import java.util.Map;
import java.util.HashMap;

/**
 * Query manager execution handler implementation
 */
public class QMExecutionHandlerImpl implements QMExecutionHandler, IDataSourceListener {

    private DataSourceRegistry dataSourceRegistry;
    private Map<DBPDataSource, QMDataSourceMetaInfo> dataSourcesInfo = new HashMap<DBPDataSource, QMDataSourceMetaInfo>();

    public QMExecutionHandlerImpl(DataSourceRegistry dataSourceRegistry)
    {
        this.dataSourceRegistry = dataSourceRegistry;
        this.dataSourceRegistry.addDataSourceListener(this);
    }

    public void handleTransactionCommit(DBCExecutionContext context)
    {
        
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

    public void handleDataSourceEvent(DataSourceEvent event)
    {
        switch (event.getAction()) {
        case CONNECT:
            dataSourcesInfo.put(event.getDataSource().getDataSource(), new QMDataSourceMetaInfo());
            break;
        case DISCONNECT:
            dataSourcesInfo.remove(event.getDataSource().getDataSource());
            break;
        default:
            // doesn't matter
            break;
        }
    }

    public void dispose()
    {
        if (dataSourceRegistry != null) {
            dataSourceRegistry.removeDataSourceListener(this);
            dataSourceRegistry = null;
        }
    }

}
