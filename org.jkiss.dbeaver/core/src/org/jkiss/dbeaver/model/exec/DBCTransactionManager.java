/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;

/**
* DBCSession
 */
public interface DBCTransactionManager
{
    DBPDataSource getDataSource();

    DBPTransactionIsolation getTransactionIsolation() throws DBCException;

    void setTransactionIsolation(DBPTransactionIsolation transactionIsolation) throws DBCException;

    boolean isAutoCommit() throws DBCException;

    void setAutoCommit(boolean autoCommit) throws DBCException;

    boolean supportsSavepoints();

    DBCSavepoint setSavepoint(String name)
        throws DBCException;

    void releaseSavepoint(DBCSavepoint savepoint) throws DBCException;

    void commit() throws DBCException;

    void rollback(DBCSavepoint savepoint) throws DBCException;

}
