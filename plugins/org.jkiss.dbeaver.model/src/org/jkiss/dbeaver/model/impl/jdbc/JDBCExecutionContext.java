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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCSavepointImpl;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;

/**
 * JDBCExecutionContext.
 * Implements transaction manager and execution context defaults.
 * Both depend on datasource implementation.
 */
public class JDBCExecutionContext extends AbstractExecutionContext<JDBCDataSource> implements DBCTransactionManager, IAdaptable {
    public static final String TYPE_MAIN = "Main";
    public static final String TYPE_METADATA = "Metadata";

    private static final Log log = Log.getLog(JDBCExecutionContext.class);

    // Time to wait for txn level/auto-commit detection
    static final int TXN_INFO_READ_TIMEOUT = 5000;

    @NotNull
    private volatile JDBCRemoteInstance instance;
    private volatile Connection connection;
    private volatile Boolean autoCommit;
    private volatile Integer transactionIsolationLevel;

    public JDBCExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance.getDataSource(), purpose);
        this.instance = instance;
    }

    @Override
    public JDBCRemoteInstance getOwnerInstance() {
        return instance;
    }

    protected void setOwnerInstance(@NotNull JDBCRemoteInstance instance) {
        this.instance = instance;
    }

    @NotNull
    private Connection getConnection() {
        return connection;
    }

    public void connect(DBRProgressMonitor monitor) throws DBCException {
        connect(monitor, null, null, null, true);
    }

    protected void connect(@NotNull DBRProgressMonitor monitor, Boolean autoCommit, @Nullable Integer txnLevel, JDBCExecutionContext initFrom, boolean addContext) throws DBCException {
        if (connection != null && addContext) {
            log.error("Reopening not-closed connection");
            close();
        }
        boolean connectionReadOnly = dataSource.getContainer().isConnectionReadOnly();
        DBExecUtils.startContextInitiation(dataSource.getContainer());
        try {
            this.connection = dataSource.openConnection(monitor, this, purpose);
            if (this.connection == null) {
                throw new DBCException("Null connection returned");
            }
            monitor.subTask("Set connection defaults");
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
                log.debug("Can't set auto-commit state: " + e.getMessage()); //$NON-NLS-1$
            }
            {
                // Cache auto-commit
                try {
                    this.autoCommit = connection.getAutoCommit();
                } catch (Throwable e) {
                    log.debug("Can't check auto-commit state", e); //$NON-NLS-1$
                    this.autoCommit = false;
                }
            }

            if (!this.autoCommit && txnLevel != null) {
                try {
                    this.connection.setTransactionIsolation(txnLevel);
                    this.transactionIsolationLevel = txnLevel;
                } catch (Throwable e) {
                    log.debug("Can't set transaction isolation level", e); //$NON-NLS-1$
                }
            }

            try {
                this.initContextBootstrap(monitor, autoCommit);
            } catch (DBCException e) {
                log.warn("Error while running context bootstrap", e);
            }

            try {
                // Init (or copy) context state
                this.dataSource.initializeContextState(monitor, this, initFrom);
            } catch (DBException e) {
                log.warn("Error while initializing context state", e);
            }

            try {
                // Commit transaction. We can perform init SQL which potentially may lock some resources
                // Let's free them.
                if (!this.autoCommit) {
                    try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "End transaction")) {
                        session.commit();
                    }
                }
            } catch (Throwable e) {
                log.error("Error ending transaction after context initialize", e);
            }

            if (addContext) {
                // Add self to context list
                this.instance.addContext(this);
            }
        } finally {
            DBExecUtils.finishContextInitiation(dataSource.getContainer());
        }
    }

    protected void disconnect() {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        synchronized (this) {
            if (this.connection != null) {
                this.dataSource.closeConnection(connection, purpose);
            }
            this.connection = null;
        }
        // Notify QM
        super.closeContext();
    }

    @NotNull
    public Connection getConnection(DBRProgressMonitor monitor) throws SQLException {
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
    public JDBCSession openSession(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionPurpose purpose, @NotNull String taskTitle) {
        return dataSource.createConnection(monitor, this, purpose, taskTitle);
    }

    @Override
    public void checkContextAlive(DBRProgressMonitor monitor) throws DBException {
        if (!JDBCUtils.isConnectionAlive(getDataSource(), getConnection())) {
            throw new DBCException("Connection is dead");
        }
    }

    @Override
    public boolean isConnected() {
        return connection != null;
    }

    @NotNull
    @Override
    public InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor, boolean closeOnFailure)
        throws DBException {
        if (this.connection == null) {
            connect(monitor);
            return InvalidateResult.CONNECTED;
        }

        // Do not test - just reopen the tunnel. Otherwise it may take too much time.
        boolean checkOk = false;//JDBCUtils.isConnectionAlive(getDataSource(), getConnection());
        closeOnFailure = true;

        if (!checkOk) {
            Boolean prevAutocommit = autoCommit;
            Integer txnLevel = transactionIsolationLevel;
            if (closeOnFailure) {
                closeContext(false);
            }
            connect(monitor, prevAutocommit, txnLevel, this, false);

            return InvalidateResult.RECONNECTED;
        }
        return InvalidateResult.ALIVE;
    }

    @Override
    public void close() {
        closeContext(true);
    }

    private void closeContext(boolean removeContext) {
        disconnect();

        if (removeContext) {
            // Remove self from context list
            this.instance.removeContext(this);
        }
    }

    //////////////////////////////////////////////////////////////
    // Transaction manager
    //////////////////////////////////////////////////////////////

    @Override
    public DBPTransactionIsolation getTransactionIsolation()
        throws DBCException {
        if (transactionIsolationLevel == null) {
            if (!RuntimeUtils.runTask(monitor -> {
                try {
                    transactionIsolationLevel = getConnection().getTransactionIsolation();
                } catch (Throwable e) {
                    transactionIsolationLevel = Connection.TRANSACTION_NONE;
                    log.error("Error getting transaction isolation level", e);
                }
            }, "Get transaction isolation level", TXN_INFO_READ_TIMEOUT)) {
                throw new DBCException("Can't determine transaction isolation - timeout");
            }
        }
        return JDBCTransactionIsolation.getByCode(transactionIsolationLevel);
    }

    @Override
    public void setTransactionIsolation(@NotNull DBRProgressMonitor monitor, @NotNull DBPTransactionIsolation transactionIsolation)
        throws DBCException {
        if (!(transactionIsolation instanceof JDBCTransactionIsolation)) {
            throw new DBCException(ModelMessages.model_jdbc_exception_invalid_transaction_isolation_parameter);
        }
        JDBCTransactionIsolation jdbcTIL = (JDBCTransactionIsolation) transactionIsolation;
        try {
            getConnection().setTransactionIsolation(jdbcTIL.getCode());
            transactionIsolationLevel = jdbcTIL.getCode();
        } catch (SQLException e) {
            throw new JDBCException(e, this);
        } finally {
            QMUtils.getDefaultHandler().handleTransactionIsolation(this, transactionIsolation);
        }

        //QMUtils.getDefaultHandler().handleTransactionIsolation(getConnection(), jdbcTIL);
    }

    @Override
    public boolean isAutoCommit()
        throws DBCException {
        if (autoCommit == null) {
            // Run in task with timeout
            if (!RuntimeUtils.runTask(monitor -> {
                try {
                    autoCommit = getConnection().getAutoCommit();
                } catch (Exception e) {
                    log.error("Error getting auto commit state", e);
                }
            }, "Get auto commit state", TXN_INFO_READ_TIMEOUT)) {
                throw new DBCException("Can't determine auto-commit state - timeout");
            }
        }
        return autoCommit;
    }

    @Override
    public void setAutoCommit(@NotNull DBRProgressMonitor monitor, boolean autoCommit)
        throws DBCException {
        monitor.subTask("Set JDBC connection auto-commit " + autoCommit);
        try {
            connection.setAutoCommit(autoCommit);
            this.autoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            throw new JDBCException(e, this);
        } finally {
            QMUtils.getDefaultHandler().handleTransactionAutocommit(this, autoCommit);
        }
    }

    @Override
    public DBCSavepoint setSavepoint(@NotNull DBRProgressMonitor monitor, String name)
        throws DBCException {
        Savepoint savepoint;
        try {
            if (name == null) {
                savepoint = getConnection().setSavepoint();
            } else {
                savepoint = getConnection().setSavepoint(name);
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        return new JDBCSavepointImpl(this, savepoint);
    }

    @Override
    public boolean supportsSavepoints() {
        return getDataSource().getInfo().supportsSavepoints();
    }

    @Override
    public void releaseSavepoint(@NotNull DBRProgressMonitor monitor, @NotNull DBCSavepoint savepoint)
        throws DBCException {
        try {
            if (savepoint instanceof JDBCSavepointImpl) {
                getConnection().releaseSavepoint(((JDBCSavepointImpl) savepoint).getOriginal());
            } else if (savepoint instanceof Savepoint) {
                getConnection().releaseSavepoint((Savepoint) savepoint);
            } else {
                throw new SQLFeatureNotSupportedException(ModelMessages.model_jdbc_exception_bad_savepoint_object);
            }
        } catch (SQLException e) {
            throw new JDBCException(e, this);
        }
    }

    @Override
    public void commit(@NotNull DBCSession session)
        throws DBCException {
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new JDBCException(e, this);
        } finally {
            if (session.isLoggingEnabled()) {
                QMUtils.getDefaultHandler().handleTransactionCommit(this);
            }
        }
    }

    @Override
    public void rollback(@NotNull DBCSession session, DBCSavepoint savepoint)
        throws DBCException {
        try {
            if (savepoint != null) {
                if (savepoint instanceof JDBCSavepointImpl) {
                    getConnection().rollback(((JDBCSavepointImpl) savepoint).getOriginal());
                } else if (savepoint instanceof Savepoint) {
                    getConnection().rollback((Savepoint) savepoint);
                } else {
                    throw new SQLFeatureNotSupportedException(ModelMessages.model_jdbc_exception_bad_savepoint_object);
                }
            } else {
                getConnection().rollback();
            }
        } catch (SQLException e) {
            throw new JDBCException(e, this);
        } finally {
            if (session.isLoggingEnabled()) {
                QMUtils.getDefaultHandler().handleTransactionRollback(this, savepoint);
            }
        }
    }

    @Override
    public boolean isSupportsTransactions() {
        return instance.getDataSource().getInfo().supportsTransactions();
    }

    public void reconnect(DBRProgressMonitor monitor) throws DBCException {
        close();
        connect(monitor, null, null, this, true);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCTransactionManager.class) {
            return adapter.cast(this);
        }
        return null;
    }

    @Override
    public String toString() {
        if (CommonUtils.equalObjects(instance.getName(), dataSource.getName())) {
            return super.toString();
        }
        return dataSource.getName() + " - " + instance.getName() + " - " + getContextName();
    }
}
