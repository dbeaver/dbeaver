/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;

import java.sql.ResultSet;

/**
 * ResultSet container.
 * May be used as "fake" statement to wrap result sets returned by connection metadata or something.
 */
public class ResultSetStatement extends PreparedStatementManagable {

    private ResultSet resultSet;

    public ResultSetStatement(
        JDBCExecutionContext connection,
        ResultSet resultSet,
        String description)
    {
        super(connection, VoidStatement.INSTANCE, "?");
        this.resultSet = resultSet;
        setDescription(description);
    }

    public JDBCResultSet executeQuery()
    {
        return new ResultSetManagable(this, resultSet);
    }

    @Override
    public JDBCResultSet getResultSet()
    {
        return new ResultSetManagable(this, resultSet);
    }

}