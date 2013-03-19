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

    public int getTransactionIsolation() throws SQLException
    {
        if (transactionIsolationLevel == null) {
            transactionIsolationLevel = connection.getTransactionIsolation();
        }
        return transactionIsolationLevel;
    }
}
