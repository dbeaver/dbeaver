/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

import java.sql.SQLException;

/**
 * ResultSet container.
 * May be used as "fake" statement to wrap result sets returned by connection metadata or something.
 */
class JDBCFakeStatementImpl extends JDBCPreparedStatementImpl {

    private JDBCResultSetImpl resultSet;

    JDBCFakeStatementImpl(
        JDBCExecutionContext connection,
        JDBCResultSetImpl resultSet,
        String description)
    {
        super(connection, JDBCVoidStatementImpl.INSTANCE, description);
        this.resultSet = resultSet;
        setDescription(description);
    }

    @Override
    public boolean execute() throws SQLException
    {
        return false;
    }

    @Override
    public boolean executeStatement() throws DBCException
    {
        return false;
    }

    @Override
    public int executeUpdate() throws SQLException
    {
        return 0;
    }

    @Override
    public JDBCResultSet executeQuery()
    {
        return resultSet;
    }

    @Override
    public JDBCResultSet getResultSet()
    {
        return resultSet;
    }

}