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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Abstract execution context
 */
public abstract class AbstractSession implements DBCSession, DBRBlockingObject {

    private DBRProgressMonitor monitor;
    private DBCExecutionPurpose purpose;
    private String taskTitle;
    private DBDDataFormatterProfile dataFormatterProfile;

    public AbstractSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        this.monitor = monitor;
        this.purpose = purpose;
        this.taskTitle = taskTitle;

        if (taskTitle != null) {
            monitor.startBlock(this, taskTitle);
        }

        QMUtils.getDefaultHandler().handleSessionOpen(this);
    }

    @Override
    public String getTaskTitle()
    {
        return taskTitle;
    }

    @Override
    public boolean isConnected()
    {
        return true;
    }

    @Override
    public DBRProgressMonitor getProgressMonitor()
    {
        return monitor;
    }

    @Override
    public DBCTransactionManager getTransactionManager()
    {
        return new AbstractTransactionManager();
    }

    @Override
    public DBCExecutionPurpose getPurpose()
    {
        return purpose;
    }

    @Override
    public DBDDataFormatterProfile getDataFormatterProfile()
    {
        if (dataFormatterProfile == null) {
            return getDataSource().getContainer().getDataFormatterProfile();
        }
        return dataFormatterProfile;
    }

    @Override
    public void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        dataFormatterProfile = formatterProfile;
    }

    @Override
    public DBDValueHandler getDefaultValueHandler()
    {
        return DBCDefaultValueHandler.INSTANCE;
    }

    @Override
    public void close()
    {
        QMUtils.getDefaultHandler().handleSessionClose(this);
    }

    protected class AbstractTransactionManager implements DBCTransactionManager {

        @Override
        public DBPDataSource getDataSource()
        {
            return AbstractSession.this.getDataSource();
        }

        @Override
        public DBPTransactionIsolation getTransactionIsolation()
            throws DBCException
        {
            return null;
        }

        @Override
        public void setTransactionIsolation(DBPTransactionIsolation transactionIsolation)
            throws DBCException
        {
            throw new DBCException("Transaction isolation change not supported");
        }

        @Override
        public boolean isAutoCommit()
            throws DBCException
        {
            return true;
        }

        @Override
        public void setAutoCommit(boolean autoCommit)
            throws DBCException
        {
            throw new DBCException("Auto-commit change not supported");
        }

        @Override
        public boolean supportsSavepoints()
        {
            return getDataSource().getInfo().supportsSavepoints();
        }

        @Override
        public DBCSavepoint setSavepoint(String name)
            throws DBCException
        {
            throw new DBCException("Savepoint not supported");
        }

        @Override
        public void releaseSavepoint(DBCSavepoint savepoint)
            throws DBCException
        {
            throw new DBCException("Savepoint not supported");
        }

        @Override
        public void commit()
            throws DBCException
        {
            // do nothing
        }

        @Override
        public void rollback(DBCSavepoint savepoint)
            throws DBCException
        {
            throw new DBCException("Transactions not supported");
        }
    }

}
