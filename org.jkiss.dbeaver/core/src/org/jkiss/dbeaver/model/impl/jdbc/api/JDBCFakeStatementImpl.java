/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

import java.sql.ResultSet;

/**
 * ResultSet container.
 * May be used as "fake" statement to wrap result sets returned by connection metadata or something.
 */
public class JDBCFakeStatementImpl extends JDBCPreparedStatementImpl {

    private ResultSet resultSet;

    public JDBCFakeStatementImpl(
        JDBCExecutionContext connection,
        ResultSet resultSet,
        String description)
    {
        super(connection, JDBCVoidStatementImpl.INSTANCE, "?");
        this.resultSet = resultSet;
        setDescription(description);
    }

    public JDBCResultSet executeQuery()
    {
        return new JDBCResultSetImpl(this, resultSet);
    }

    @Override
    public JDBCResultSet getResultSet()
    {
        return new JDBCResultSetImpl(this, resultSet);
    }

}