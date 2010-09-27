/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;

/**
 * Default execution handler.
 * Handle methods are no-ops.
 */
public abstract class DefaultExecutionHandler implements QMExecutionHandler {

    public void handleContextOpen(DBCExecutionContext context)
    {

    }

    public void handleContextClose(DBCExecutionContext context)
    {

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

    public void handleStatementExecuteBegin(DBCStatement statement)
    {

    }

    public void handleStatementExecuteEnd(DBCStatement statement)
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

    public void handleResultSetClose(DBCResultSet resultSet)
    {

    }
}
