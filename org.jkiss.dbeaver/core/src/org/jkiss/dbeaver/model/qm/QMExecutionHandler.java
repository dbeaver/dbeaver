/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.dbc.DBCStatement;

/**
 * Query manager
 */
public interface QMExecutionHandler {

    String getHandlerName();

    void handleTransactionCommit(DBCExecutionContext context);

    void handleTransactionSavepoint(DBCSavepoint savepoint);

    void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint);

    void handleStatementOpen(DBCStatement statement);

    void handleStatementExecute(DBCStatement statement);

    void handleStatementBind(DBCStatement statement, Object column, Object value);

    void handleStatementClose(DBCStatement statement);

    void handleResultSetOpen(DBCResultSet resultSet);

    void handleResultSetFetch(DBCResultSet resultSet);

    void handleResultSetClose(DBCResultSet resultSet);

}
