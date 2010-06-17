/*
* Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
*/

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;

/**
* DBCSession
 */
public interface DBPTransactionManager
{
    DBPDataSource getDataSource();

    DBPTransactionIsolation getTransactionIsolation() throws DBCException;

    void setTransactionIsolation(DBPTransactionIsolation transactionIsolation) throws DBCException;

    boolean isAutoCommit() throws DBCException;

    void setAutoCommit(boolean autoCommit) throws DBCException;

    void commit() throws DBCException;

    void rollback(DBCSavepoint savepoint) throws DBCException;

}
