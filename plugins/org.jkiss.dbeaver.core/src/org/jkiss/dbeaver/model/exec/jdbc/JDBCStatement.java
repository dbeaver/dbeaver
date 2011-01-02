/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    JDBCExecutionContext getConnection();

    JDBCResultSet openResultSet() throws DBCException;

    JDBCResultSet openGeneratedKeysResultSet() throws DBCException;

    JDBCResultSet executeQuery(String sql)
        throws SQLException;

    JDBCResultSet getResultSet()
        throws SQLException;

    void close();
}
