/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCSavepointImpl;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JDBCExecutionContext.
 * Implements transaction manager and execution context defaults.
 * Both depend on datasource implementation.
 */
public class JDBCExecutionContext extends AbstractExecutionContext<JDBCDataSource> implements DBCTransactionManager, DBPAdaptable {
    public static final String TYPE_MAIN = "Main";
    public static final String TYPE_METADATA = "Metadata";

    protected static final String TASK_TITLE_SET_SCHEMA = "Set active schema"; //$NON-NLS-1$

    private static final Log log = Log.getLog(JDBCExecutionContext.class);

    // Time to wait for txn level/auto-commit detection
    static final int TXN_INFO_READ_TIMEOUT = 5000;

    @NotNull
    private volatile JDBCRemoteInstance instance;
    private volatile Connection connection;
    private volatile Boolean autoCommit;
    private volatile Integer transactionIsolationLevel;
    private transient volatile boolean txnIsolationLevelReadInProgress;
    private final ReentrantLock queryExecutionLock;

    public JDBCExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance.getDataSource(), purpose);
        this.instance = instance;
        if (!instance.getDataSource().getContainer().getDriver().isThreadSafeDriver()) {
            queryExecutionLock = new ReentrantLock();
        } else {
            queryExecutionLock = null;
        }
    }

    public JDBCExecutionContext(@NotNull JDBCRemoteInstance instance, boolean test) {
        super(instance.getDataSource(), "Test for " + instance);
        this.instance = instance;
        queryExecutionLock = null;
    }

    @Override
    public JDBCRemoteInstance getOwnerInstance() {
        return instance;
    }

    protected void setOwnerInstance(@NotNull JDBCRemoteInstance instance) {
        this.instance = instance;
    }

    @NotNull
    private Connection getConnection() throws DBCException {
        Connection dbCon = this.connection;
        if (dbCon == null) {
            throw new DBCException("Disconnected");
        }
        return dbCon;
    }

    @Nullable
    public Connection getConnectionOrNull() {
        return this.connection;
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
        final JDBCRemoteInstance currentInstance = this.instance;

        DBExecUtils.startContextInitiation(dataSource.getContainer());

        Object exclusiveLock = currentInstance.getExclusiveLock().acquireExclusiveLock();
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

            if (txnLevel != null) {
                try {
                    this.getConnection().setTransactionIsolation(txnLevel);
                    this.transactionIsolationLevel = txnLevel;
                } catch (Throwable e) {
                    log.debug("Can't set transaction isolation level", e); //$NON-NLS-1$
                }
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

            try {
                this.initContextBootstrap(monitor, autoCommit);
            } catch (DBCException e) {
                log.warn("Error while running context bootstrap", e);
            }

            if (addContext) {
                // Add self to context list
                currentInstance.addContext(this);
            }
        } finally {
            DBExecUtils.finishContextInitiation(dataSource.getContainer());
            currentInstance.getExclusiveLock().releaseExclusiveLock(exclusiveLock);
        }

        // Now initialize context state
        // Do it outside of exclusive lock to avoid dead locks
        {
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
                    try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Start transaction")) {
                        session.enableLogging(false); // Disable logging to avoid smart commit recovery activation
                        session.commit();
                    }
                }
            } catch (Throwable e) {
                log.error("Error ending transaction after context initialize", e);
            }
        }

    }

    protected void disconnect() {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        synchronized (this) {
            // If we cannot determine if connection is in autocommit mode, assume that it is not
            if (connection != null && !dataSource.closeConnection(connection, purpose, !isAutoCommit(false))) {
                log.debug("Connection close timeout");
            }
            this.connection = null;
        }
        // Notify QM
        super.closeContext();
    }

    @NotNull
    public Connection getConnection(DBRProgressMonitor monitor) throws SQLException {
        Connection result = getConnection(monitor, true);
        if (result == null) {
            throw new SQLException("Null connection returned");
        }
        return result;
    }

    @Nullable
    public Connection getConnection(DBRProgressMonitor monitor, boolean openIfNeeded) throws SQLException {
        if (connection == null && openIfNeeded) {
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

    @Override
    public void invalidateContext(@NotNull DBRProgressMonitor monitor, @NotNull DBCInvalidatePhase phase) throws DBException {
        if (phase == DBCInvalidatePhase.BEFORE_INVALIDATE) {
            closeContext(false);
        }

        if (phase == DBCInvalidatePhase.INVALIDATE) {
            // Try to connect again.
            // If connect will fail then context will remain in the list but with null connection.
            // On next invalidate it will try to reopen
            connect(monitor, autoCommit, transactionIsolationLevel, this, false);
        }
    }

    @Override
    public void close() {
        closeContext(true);
    }

    private void closeContext(boolean removeContext) {
        // We remove context before it is actually closed.
        // Because disconnect may (potentially) hang in socket forever
        if (removeContext) {
            // Remove self from context list
            this.instance.removeContext(this);
        }

        disconnect();
    }

    //////////////////////////////////////////////////////////////
    // Transaction manager
    //////////////////////////////////////////////////////////////

    @Override
    public DBPTransactionIsolation getTransactionIsolation() throws DBCException {
        if (transactionIsolationLevel == null) {
            if (isAutoCommit()) {
                return JDBCTransactionIsolation.getByCode(Connection.TRANSACTION_NONE);
            }
            if (!txnIsolationLevelReadInProgress) {
                txnIsolationLevelReadInProgress = true;
                new AbstractJob("Get transaction isolation level") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        try {
                            DBExecUtils.tryExecuteRecover(monitor, getDataSource(), monitor1 -> {
                                try {
                                    transactionIsolationLevel = getConnection().getTransactionIsolation();
                                } catch (Throwable e) {
                                    transactionIsolationLevel = Connection.TRANSACTION_NONE;
                                    log.error("Error getting transaction isolation level", e);
                                }
                            });
                        } catch (DBException e) {
                            log.debug("Error determining transaction isolation level", e);
                        } finally {
                            txnIsolationLevelReadInProgress = false;
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
            if (transactionIsolationLevel == null) {
                return JDBCTransactionIsolation.getByCode(Connection.TRANSACTION_NONE);
                //log.error("Cannot determine transaction isolation level due to connection hanging. Setting to NONE.");
            }
        }
        return JDBCTransactionIsolation.getByCode(transactionIsolationLevel);
    }

    @Override
    public void setTransactionIsolation(@NotNull DBRProgressMonitor monitor, @NotNull DBPTransactionIsolation transactionIsolation)
        throws DBCException {
        if (!(transactionIsolation instanceof JDBCTransactionIsolation jdbcTIL)) {
            throw new DBCException(ModelMessages.model_jdbc_exception_invalid_transaction_isolation_parameter);
        }
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
                    DBExecUtils.tryExecuteRecover(monitor, getDataSource(), monitor1 -> {
                        try {
                            autoCommit = getConnection().getAutoCommit();
                            if (!autoCommit) {
                                try {
                                    transactionIsolationLevel = getConnection().getTransactionIsolation();
                                } catch (Throwable e) {
                                    transactionIsolationLevel = Connection.TRANSACTION_NONE;
                                    log.debug("Error getting transaction isolation level: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error getting auto commit state", e);
                        }
                    });
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }, "Get auto commit state", TXN_INFO_READ_TIMEOUT)) {
                throw new DBCException("Can't determine auto-commit state - timeout");
            }
            if (autoCommit == null) {
                log.error("Cannot determine autocommit state due to connection hanging. Setting to manual commit mode.");
                autoCommit = false;
            }
        }
        return autoCommit;
    }

    private boolean isAutoCommit(boolean defaultValue) {
        try {
            return isAutoCommit();
        } catch (DBCException e) {
            log.debug("Unable to determine if connection is in autocommit mode", e);
            return defaultValue;
        }
    }

    @Override
    public void setAutoCommit(@NotNull DBRProgressMonitor monitor, boolean autoCommit)
        throws DBCException {
        if (this.autoCommit != null && this.autoCommit == autoCommit) {
            return;
        }
        monitor.subTask("Set JDBC connection auto-commit " + autoCommit);
        try {
            Connection dbCon = getConnection();
            dbCon.setAutoCommit(autoCommit);
            this.autoCommit = dbCon.getAutoCommit();
            if (!this.autoCommit) {
                try {
                    transactionIsolationLevel = getConnection().getTransactionIsolation();
                } catch (Throwable e) {
                    transactionIsolationLevel = Connection.TRANSACTION_NONE;
                    log.debug("Error getting transaction isolation level: " + e.getMessage());
                }
            } else {
                transactionIsolationLevel = null;
            }
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
            Connection dbCon = getConnection();
            if (name == null) {
                savepoint = dbCon.setSavepoint();
            } else {
                savepoint = dbCon.setSavepoint(name);
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
            Connection dbCon = getConnection();
            if (savepoint instanceof JDBCSavepointImpl) {
                dbCon.releaseSavepoint(((JDBCSavepointImpl) savepoint).getOriginal());
            } else if (savepoint instanceof Savepoint) {
                dbCon.releaseSavepoint((Savepoint) savepoint);
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
            Connection dbCon = getConnection();
            if (savepoint != null) {
                if (savepoint instanceof JDBCSavepointImpl) {
                    dbCon.rollback(((JDBCSavepointImpl) savepoint).getOriginal());
                } else if (savepoint instanceof Savepoint) {
                    dbCon.rollback((Savepoint) savepoint);
                } else {
                    throw new SQLFeatureNotSupportedException(ModelMessages.model_jdbc_exception_bad_savepoint_object);
                }
            } else {
                dbCon.rollback();
            }
        } catch (SQLException e) {
            if (JDBCUtils.isRollbackWarning(e)) {
                log.debug("Rollback warning: " + e.getMessage());
            } else {
                throw new JDBCException(e, this);
            }
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

    /**
     * Acquires lock on connection level.
     * Any other thread will wait until you release lock with @unlockQueryExecution
     */
    public void lockQueryExecution() {
        if (this.queryExecutionLock != null) {
            this.queryExecutionLock.lock();
        }
    }

    /**
     * Release lock. Must be called in finally.
     */
    public void unlockQueryExecution() {
        if (this.queryExecutionLock != null) {
            this.queryExecutionLock.unlock();
        }
    }

}
