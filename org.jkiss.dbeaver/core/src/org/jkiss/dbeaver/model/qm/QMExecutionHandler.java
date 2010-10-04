/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;

/**
 * Query manager execution handler.
 * Handler methods are invoked right at time of DBC operation, so they should work as fast as possible.
 * Implementers should not invoke any DBC execution function in passed objects - otherwise execution handling may enter infinite recursion.
 */
public interface QMExecutionHandler {

    String getHandlerName();

    void handleSessionStart(DBPDataSource dataSource, boolean transactional);

    void handleSessionEnd(DBPDataSource dataSource);

    void handleContextOpen(DBCExecutionContext context);

    void handleContextClose(DBCExecutionContext context);

    void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit);

    void handleTransactionIsolation(DBCExecutionContext context, DBPTransactionIsolation level);

    void handleTransactionCommit(DBCExecutionContext context);

    void handleTransactionSavepoint(DBCSavepoint savepoint);

    void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint);

    void handleStatementOpen(DBCStatement statement);

    void handleStatementExecuteBegin(DBCStatement statement);

    void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error);

    void handleStatementBind(DBCStatement statement, Object column, Object value);

    void handleStatementClose(DBCStatement statement);

    void handleResultSetOpen(DBCResultSet resultSet);

    void handleResultSetClose(DBCResultSet resultSet, long rowCount);

    void handleScriptBegin(DBCExecutionContext context);
    
    void handleScriptEnd(DBCExecutionContext context);

}
