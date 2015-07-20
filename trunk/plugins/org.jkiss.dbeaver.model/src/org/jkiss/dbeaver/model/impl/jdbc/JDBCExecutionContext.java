/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCSavepointImpl;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;

/**
 * JDBCExecutionContext
 */
public class JDBCExecutionContext extends AbstractExecutionContext<JDBCDataSource> implements DBCTransactionManager
{
    static final Log log = Log.getLog(JDBCExecutionContext.class);

    private volatile Connection connection;
    private volatile Boolean autoCommit;
    private volatile Integer transactionIsolationLevel;

    public JDBCExecutionContext(@NotNull JDBCDataSource dataSource, String purpose)
    {
        super(dataSource, purpose);
    }

    @NotNull
    private Connection getConnection() {
        return connection;
    }

    public void connect(DBRProgressMonitor monitor) throws DBCException
    {
        connect(monitor, null, null, false);
    }

    void connect(DBRProgressMonitor monitor, Boolean autoCommit, @Nullable Integer txnLevel, boolean forceActiveObject) throws DBCException
    {
        if (connection != null) {
            log.error("Reopening not-closed connection");
            close();
        }
        boolean connectionReadOnly = dataSource.getContainer().isConnectionReadOnly();
        ACTIVE_CONTEXT.set(this);
        try {
            this.connection = dataSource.openConnection(monitor, purpose);
            if (this.connection == null) {
                throw new DBCException("Null connection returned");
            }

            // Get defaults from preferences
            if (autoCommit == null) {
                autoCommit = dataSource.getContainer().isDefaultAutoCommit();
            }
            // Get default txn isolation level
            if (txnLevel == null) {
                txnLevel = dataSource.getContainer().getDefaultTransactionsIsolation();
            }

            try {
                connection.setAutoCommit(autoCommit);
                this.autoCommit = autoCommit;
            } catch (Throwable e) {
                log.warn("Can't set auto-commit state", e); //$NON-NLS-1$
            }
            {
                // Cache auto-commit
                try {
                    this.autoCommit = connection.getAutoCommit();
                } catch (Throwable e) {
                    log.warn("Can't check auto-commit state", e); //$NON-NLS-1$
                    this.autoCommit = false;
                }
            }

            if (!this.autoCommit && txnLevel != null) {
                try {
                    connection.setTransactionIsolation(txnLevel);
                    this.transactionIsolationLevel = txnLevel;
                } catch (Throwable e) {
                    log.warn("Can't set transaction isolation level", e); //$NON-NLS-1$
                }
            }

            try {
                this.initContextBootstrap(monitor, autoCommit);
            } catch (DBCException e) {
                log.error("Error while running context bootstrap", e);
            }

            try {
                // Copy context state
                this.dataSource.initializeContextState(monitor, this, forceActiveObject && !connectionReadOnly);
            } catch (DBCException e) {
                log.error("Error while initializing context state", e);
            }

            // Add self to context list
            this.dataSource.allContexts.add(this);
        } finally {
            ACTIVE_CONTEXT.remove();
        }
    }

    public @NotNull Connection getConnection(DBRProgressMonitor monitor) throws SQLException
    {
        if (connection == null) {
            try {
                connect(monitor);
            } catch (DBCException e) {
                if (e.getCause() instanceof SQLException) {
                    throw (SQLException) e.getCause();
                } else {
                    throw new SQLException(e);
                }
            }
        }
        return connection;
    }

    @NotNull
    @Override
    public JDBCSession openSession(@NotNull DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        return dataSource.createConnection(monitor, this, purpose, taskTitle);
    }

    @Override
    public boolean isConnected()
    {
        return connection != null;
    }

    @Override
    public InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.connection == null) {
            connect(monitor);
            return InvalidateResult.CONNECTED;
        }

        if (!JDBCUtils.isConnectionAlive(getConnection())) {
            Boolean prevAutocommit = autoCommit;
            Integer txnLevel = transactionIsolationLevel;
            close();
            connect(monitor, prevAutocommit, txnLevel, true);

            return InvalidateResult.RECONNECTED;
        }
        return InvalidateResult.ALIVE;
    }

    @Override
    public void close()
    {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        synchronized (this) {
            if (connection != null) {
                this.dataSource.closeConnection(connection);
                connection = null;
            }
            super.closeContext();
        }

        // Remove self from context list
        this.dataSource.allContexts.remove(this);

    }

    //////////////////////////////////////////////////////////////
    // Transaction manager
    //////////////////////////////////////////////////////////////

    @Override
    public DBPTransactionIsolation getTransactionIsolation()
        throws DBCException
    {
        try {
            if (transactionIsolationLevel == null) {
                transactionIsolationLevel = getConnection().getTransactionIsolation();
            }
            return JDBCTransactionIsolation.getByCode(transactionIsolationLevel);
        } catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        }
    }

    @Override
    public void setTransactionIsolation(@NotNull DBRProgressMonitor monitor, @NotNull DBPTransactionIsolation transactionIsolation)
        throws DBCException
    {
        if (!(transactionIsolation instanceof JDBCTransactionIsolation)) {
            throw new DBCException(ModelMessages.model_jdbc_exception_invalid_transaction_isolation_parameter);
        }
        JDBCTransactionIsolation jdbcTIL = (JDBCTransactionIsolation) transactionIsolation;
        try {
            getConnection().setTransactionIsolation(jdbcTIL.getCode());
            transactionIsolationLevel = jdbcTIL.getCode();
        } catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        } finally {
            QMUtils.getDefaultHandler().handleTransactionIsolation(this, transactionIsolation);
        }

        //QMUtils.getDefaultHandler().handleTransactionIsolation(getConnection(), jdbcTIL);
    }

    @Override
    public boolean isAutoCommit()
        throws DBCException
    {
        try {
            if (autoCommit == null) {
                autoCommit = getConnection().getAutoCommit();
            }
            return autoCommit;
        }
        catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        }
    }

    @Override
    public void setAutoCommit(@NotNull DBRProgressMonitor monitor, boolean autoCommit)
        throws DBCException
    {
        monitor.subTask("Set JDBC connection auto-commit " + autoCommit);
        try {
            connection.setAutoCommit(autoCommit);
            this.autoCommit = null;
        }
        catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        } finally {
            QMUtils.getDefaultHandler().handleTransactionAutocommit(this, autoCommit);
        }
    }

    @Override
    public DBCSavepoint setSavepoint(@NotNull DBRProgressMonitor monitor, String name)
        throws DBCException
    {
        Savepoint savepoint;
        try {
            if (name == null) {
                savepoint = getConnection().setSavepoint();
            } else {
                savepoint = getConnection().setSavepoint(name);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
        return new JDBCSavepointImpl(this, savepoint);
    }

    @Override
    public boolean supportsSavepoints()
    {
        try {
            return getConnection().getMetaData().supportsSavepoints();
        }
        catch (SQLException e) {
            // ignore
            return false;
        }
    }

    @Override
    public void releaseSavepoint(@NotNull DBRProgressMonitor monitor, @NotNull DBCSavepoint savepoint)
        throws DBCException
    {
        try {
            if (savepoint instanceof Savepoint) {
                getConnection().releaseSavepoint((Savepoint) savepoint);
            } else {
                throw new SQLFeatureNotSupportedException(ModelMessages.model_jdbc_exception_bad_savepoint_object);
            }
        }
        catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        }
    }

    @Override
    public void commit(@NotNull DBCSession session)
        throws DBCException
    {
        try {
            getConnection().commit();
        }
        catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        }
        finally {
            if (session.isLoggingEnabled()) {
                QMUtils.getDefaultHandler().handleTransactionCommit(this);
            }
        }
    }

    @Override
    public void rollback(@NotNull DBCSession session, DBCSavepoint savepoint)
        throws DBCException
    {
        try {
            if (savepoint != null) {
                if (savepoint instanceof Savepoint) {
                    getConnection().rollback((Savepoint) savepoint);
                } else {
                    throw new SQLFeatureNotSupportedException(ModelMessages.model_jdbc_exception_bad_savepoint_object);
                }
            } else {
                getConnection().rollback();
            }
        }
        catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        }
        finally {
            if (session.isLoggingEnabled()) {
                QMUtils.getDefaultHandler().handleTransactionRollback(this, savepoint);
            }
        }
    }

}
