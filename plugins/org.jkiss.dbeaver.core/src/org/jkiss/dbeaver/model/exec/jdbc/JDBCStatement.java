/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC statement
 */
public interface JDBCStatement extends Statement, DBCStatement, DBRBlockingObject {

    @Override
    JDBCExecutionContext getConnection();

    @Override
    JDBCResultSet openResultSet() throws DBCException;

    @Override
    JDBCResultSet openGeneratedKeysResultSet() throws DBCException;

    @Override
    JDBCResultSet executeQuery(String sql)
        throws SQLException;

    @Override
    JDBCResultSet getResultSet()
        throws SQLException;

    @Override
    void close();
}
