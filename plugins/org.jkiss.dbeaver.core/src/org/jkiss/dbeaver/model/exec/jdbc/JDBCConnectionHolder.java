/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.exec.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection holder.
 * Caches some connection properties (like autocommit)
 * to avoid UI block in case synchronized connections' methods.
 */
public class JDBCConnectionHolder {

    private final Connection connection;
    private volatile Boolean autoCommit;
    private volatile Integer transactionIsolationLevel;

    public JDBCConnectionHolder(Connection connection)
    {
        this.connection = connection;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public Boolean getAutoCommitCache()
    {
        return autoCommit;
    }

    public boolean getAutoCommit() throws SQLException
    {
        if (autoCommit == null) {
            autoCommit = connection.getAutoCommit();
        }
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        this.connection.setAutoCommit(autoCommit);
        this.autoCommit = null;
    }

    public void setTransactionIsolation(int level) throws SQLException
    {
        connection.setTransactionIsolation(level);
        transactionIsolationLevel = null;
    }

    public Integer getTransactionIsolationCache()
    {
        return transactionIsolationLevel;
    }

    public int getTransactionIsolation() throws SQLException
    {
        if (transactionIsolationLevel == null) {
            transactionIsolationLevel = connection.getTransactionIsolation();
        }
        return transactionIsolationLevel;
    }

    public void close() throws SQLException
    {
        if (!connection.isClosed()) {
            connection.close();
        }
    }
}
