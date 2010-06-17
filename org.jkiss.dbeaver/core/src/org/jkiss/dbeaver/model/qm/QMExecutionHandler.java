/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * Query manager
 */
public interface QMExecutionHandler {

    void handleDataSourceConnect(DBPDataSource dataSource);

    void handleDataSourceDisconnect(DBPDataSource dataSource);

    void handleTransactionCommit(DBCExecutionContext session);

    void handleTransactionSavepoint(DBCSavepoint savepoint);

    void handleTransactionRollback(DBCExecutionContext session, DBCSavepoint savepoint);

    void handleStatementOpen(DBCStatement statement);

    void handleStatementExecute(DBCStatement statement);

    void handleStatementBind(DBCStatement statement, Object column, Object value);

    void handleStatementClose(DBCStatement statement);

    void handleResultSetOpen(DBCResultSet resultSet);

    void handleResultSetFetch(DBCResultSet resultSet);

    void handleResultSetClose(DBCResultSet resultSet);

}
