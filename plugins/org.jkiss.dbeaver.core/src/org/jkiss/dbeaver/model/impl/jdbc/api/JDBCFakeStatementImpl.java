/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

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