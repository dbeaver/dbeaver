/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DBCSession
 */
public interface DBCSession extends DBRBlockingObject
{
    DBPDataSource getDataSource();

    DBPTransactionIsolation getTransactionIsolation() throws DBCException;

    void setTransactionIsolation(DBPTransactionIsolation transactionIsolation) throws DBCException;

    boolean isAutoCommit() throws DBCException;

    void setAutoCommit(boolean autoCommit) throws DBCException;

    DBCStatement prepareStatement(DBRProgressMonitor monitor, String sqlQuery, boolean scrollable, boolean updatable) throws DBCException;

    void commit() throws DBCException;

    void rollback() throws DBCException;

    void close() throws DBCException;

}
