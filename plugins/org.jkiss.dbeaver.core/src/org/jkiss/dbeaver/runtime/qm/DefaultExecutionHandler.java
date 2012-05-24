/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;

/**
 * Default execution handler.
 * Handle methods are no-ops.
 */
public abstract class DefaultExecutionHandler implements QMExecutionHandler {

    @Override
    public void handleSessionStart(DBPDataSource dataSource, boolean transactional)
    {

    }

    @Override
    public void handleSessionEnd(DBPDataSource dataSource)
    {

    }

    @Override
    public void handleContextOpen(DBCExecutionContext context)
    {

    }

    @Override
    public void handleContextClose(DBCExecutionContext context)
    {

    }

    @Override
    public void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit)
    {

    }

    @Override
    public void handleTransactionIsolation(DBCExecutionContext context, DBPTransactionIsolation level)
    {

    }

    @Override
    public void handleTransactionCommit(DBCExecutionContext context)
    {

    }

    @Override
    public void handleTransactionSavepoint(DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleStatementOpen(DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteBegin(DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error)
    {
        
    }

    @Override
    public void handleStatementBind(DBCStatement statement, Object column, Object value)
    {

    }

    @Override
    public void handleStatementClose(DBCStatement statement)
    {

    }

    @Override
    public void handleResultSetOpen(DBCResultSet resultSet)
    {

    }

    @Override
    public void handleResultSetClose(DBCResultSet resultSet, long rowCount)
    {

    }

    @Override
    public void handleScriptBegin(DBCExecutionContext context)
    {

    }

    @Override
    public void handleScriptEnd(DBCExecutionContext context)
    {

    }
}
