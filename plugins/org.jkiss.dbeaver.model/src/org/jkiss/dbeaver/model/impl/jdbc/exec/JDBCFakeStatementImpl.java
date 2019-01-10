/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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