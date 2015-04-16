/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCSavepointImpl;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;

/**
 * JDBCExecutionContext
 */
public class JDBCExecutionContext implements DBCExecutionContext, DBCTransactionManager
{
    static final Log log = Log.getLog(JDBCExecutionContext.class);

    @NotNull
    private final JDBCDataSource dataSource;
    private final boolean copyState;
    private volatile Connection connection;
    private final String purpose;
    private volatile Boolean autoCommit;
    private volatile Integer transactionIsolationLevel;

    public JDBCExecutionContext(@NotNull JDBCDataSource dataSource, String purpose, boolean copyState)
    {
        this.dataSource = dataSource;
        this.purpose = purpose;
        this.copyState = copyState;
    }

    private Connection getConnection() {
        return connection;
    }

    public void connect(DBRProgressMonitor monitor) throws DBCException
    {
        connect(monitor, null, null);
    }

    public void connect(DBRProgressMonitor monitor, Boolean autoCommit, @Nullable Integer txnLevel) throws DBCException
    {
        if (connection != null) {
            log.error("Reopening not-closed connection");
            close();
        }
        ACTIVE_CONTEXT.set(this);
        try {
            this.connection = dataSource.openConnection(monitor, purpose);
            if (autoCommit != null) {
                try {
                    connection.setAutoCommit(autoCommit);
                    this.autoCommit = autoCommit;
                } catch (Throwable e) {
                    log.warn("Could not set auto-commit state", e); //$NON-NLS-1$
                }
            }
            if (txnLevel != null) {
                try {
                    connection.setTransactionIsolation(txnLevel);
                    this.transactionIsolationLevel = txnLevel;
                } catch (Throwable e) {
                    log.warn("Could not set transaction isolation level", e); //$NON-NLS-1$
                }
            }
            if (copyState) {
                // Set active object
                if (dataSource instanceof DBSObjectSelector) {
    //                ((DBSObjectSelector) dataSource).selectObject();
    //                ((DBSObjectSelector) dataSource).getSelectedObject()
                }
            }
            {
                // Notify QM
                try {
                    this.autoCommit = connection.getAutoCommit();
                } catch (Throwable e) {
                    log.warn("Could not check auto-commit state", e); //$NON-NLS-1$
                }
                QMUtils.getDefaultHandler().handleContextOpen(this, !this.autoCommit);
            }
        } finally {
            ACTIVE_CONTEXT.remove();
        }
    }

    public Connection getConnection(DBRProgressMonitor monitor) throws SQLException
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

    @Override
    public JDBCSession openSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        return dataSource.createConnection(monitor, this, purpose, taskTitle);
    }

    @Override
    public String getContextName() {
        return purpose;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isConnected()
    {
        return connection != null;
    }

    @Override
    public InvalidateResult invalidateContext(DBRProgressMonitor monitor)
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
            connect(monitor, prevAutocommit, txnLevel);
            invalidateState(monitor);
            return InvalidateResult.RECONNECTED;
        }
        return InvalidateResult.ALIVE;
    }

    protected void invalidateState(DBRProgressMonitor monitor)
    {
        DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, this);
        if (objectSelector != null && objectSelector.supportsObjectSelect()) {
            DBSObject selectedObject = objectSelector.getSelectedObject();
            if (selectedObject != null) {
                try {
                    objectSelector.selectObject(monitor, selectedObject);
                } catch (DBException e) {
                    log.warn("Can't select object '" + selectedObject.getName() + "'", e);
                }
            }
        }
    }

    @Override
    public void close()
    {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        synchronized (this) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
            if (connection != null) {
                try {
                    connection.close();
                }
                catch (Throwable ex) {
                    log.error(ex);
                }
                QMUtils.getDefaultHandler().handleContextClose(this);
                connection = null;
            }
        }
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
    public void setTransactionIsolation(DBRProgressMonitor monitor, DBPTransactionIsolation transactionIsolation)
        throws DBCException
    {
        if (!(transactionIsolation instanceof JDBCTransactionIsolation)) {
            throw new DBCException(CoreMessages.model_jdbc_exception_invalid_transaction_isolation_parameter);
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
    public void setAutoCommit(DBRProgressMonitor monitor, boolean autoCommit)
        throws DBCException
    {
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
    public DBCSavepoint setSavepoint(DBRProgressMonitor monitor, String name)
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
    public void releaseSavepoint(DBRProgressMonitor monitor, DBCSavepoint savepoint)
        throws DBCException
    {
        try {
            if (savepoint instanceof Savepoint) {
                getConnection().releaseSavepoint((Savepoint) savepoint);
            } else {
                throw new SQLFeatureNotSupportedException(CoreMessages.model_jdbc_exception_bad_savepoint_object);
            }
        }
        catch (SQLException e) {
            throw new JDBCException(e, dataSource);
        }
    }

    @Override
    public void commit(DBCSession session)
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
    public void rollback(DBCSession session, DBCSavepoint savepoint)
        throws DBCException
    {
        try {
            if (savepoint != null) {
                if (savepoint instanceof Savepoint) {
                    getConnection().rollback((Savepoint) savepoint);
                } else {
                    throw new SQLFeatureNotSupportedException(CoreMessages.model_jdbc_exception_bad_savepoint_object);
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
