/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.jdbc;

import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.dbc.DBCQueryPurpose;
import org.jkiss.dbeaver.model.dbc.DBCStatement;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC statement
 */
public interface JDBCStatement extends Statement, DBCStatement, DBRBlockingObject {

    JDBCExecutionContext getConnection();

    String getQuery();

    String getDescription();

    JDBCStatement setDescription(String description);

    DBCQueryPurpose getQueryPurpose();

    JDBCStatement setQueryPurpose(DBCQueryPurpose queryPurpose);

    JDBCResultSet executeQuery(String sql)
        throws SQLException;

    JDBCResultSet getResultSet()
        throws SQLException;

    void close();
}
