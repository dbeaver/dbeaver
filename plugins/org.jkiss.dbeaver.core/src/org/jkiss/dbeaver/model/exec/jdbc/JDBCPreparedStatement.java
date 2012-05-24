/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC statement
 */
public interface JDBCPreparedStatement extends PreparedStatement, JDBCStatement {

    PreparedStatement getOriginal();

    @Override
    JDBCResultSet executeQuery()
        throws SQLException;

    @Override
    void close();

}