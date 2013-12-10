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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnectionHolder;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;

import java.sql.SQLException;

/**
 * JDBCExecutionContext
 */
public class JDBCExecutionContext implements DBCExecutionContext, JDBCConnector
{
    static final Log log = LogFactory.getLog(JDBCExecutionContext.class);

    private final JDBCDataSource dataSource;
    private final boolean copyState;
    private volatile JDBCConnectionHolder connectionHolder;
    private final String purpose;

    public JDBCExecutionContext(JDBCDataSource dataSource, String purpose, boolean copyState)
    {
        this.dataSource = dataSource;
        this.purpose = purpose;
        this.copyState = copyState;
    }

    public void connect(DBRProgressMonitor monitor) throws DBCException
    {
        connect(monitor, null, null);
    }

    public void connect(DBRProgressMonitor monitor, Boolean autoCommit, Integer txnLevel) throws DBCException
    {
        if (connectionHolder != null) {
            log.error("Reopening not-closed connection");
            close();
        }
        this.connectionHolder = dataSource.openConnection(monitor, purpose);
        if (autoCommit != null) {
            try {
                connectionHolder.setAutoCommit(autoCommit);
            } catch (Throwable e) {
                log.warn("Could not set auto-commit state", e); //$NON-NLS-1$
            }
        }
        if (txnLevel != null) {
            try {
                connectionHolder.setTransactionIsolation(txnLevel);
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
            boolean autoCommitState = false;
            try {
                autoCommitState = connectionHolder.getAutoCommit();
            } catch (Throwable e) {
                log.warn("Could not check auto-commit state", e); //$NON-NLS-1$
            }
            QMUtils.getDefaultHandler().handleContextOpen(this, !autoCommitState);
        }
    }

    @Override
    public JDBCConnectionHolder getConnection(DBRProgressMonitor monitor) throws SQLException
    {
        if (connectionHolder == null) {
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
        return connectionHolder;
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

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isConnected()
    {
        return connectionHolder != null && connectionHolder.getConnection() != null;
    }

    @Override
    public void invalidateContext(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.connectionHolder == null) {
            connect(monitor);
            return;
        }

        if (!JDBCUtils.isConnectionAlive(this.connectionHolder.getConnection())) {
            Boolean prevAutocommit = connectionHolder.getAutoCommitCache();
            Integer txnLevel = connectionHolder.getTransactionIsolationCache();
            close();
            connect(monitor, prevAutocommit, txnLevel);
            invalidateState(monitor);
        }
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
            if (connectionHolder != null) {
                try {
                    connectionHolder.close();
                }
                catch (Throwable ex) {
                    log.error(ex);
                }
                QMUtils.getDefaultHandler().handleContextClose(this);
                connectionHolder = null;
            }
        }
    }

}
