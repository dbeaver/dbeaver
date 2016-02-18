/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;

/**
 * ResultSet container.
 * May be used as "fake" statement to wrap result sets returned by connection metadata or something.
 */
class JDBCFakeStatementImpl extends JDBCPreparedStatementImpl {

    private JDBCResultSetImpl resultSet;
    private boolean closed;

    JDBCFakeStatementImpl(
        JDBCSession connection,
        JDBCResultSetImpl resultSet,
        String description,
        boolean disableLogging)
    {
        super(connection, JDBCVoidStatementImpl.INSTANCE, description, disableLogging);
        this.resultSet = resultSet;
        setQueryString(description);
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

    @Override
    public void close() {
        // Fake statements can be closed twice (explicitly and by owner resultset close)
        // So do real close only once
        if (!closed) {
            super.close();
            closed = true;
        }
    }
}