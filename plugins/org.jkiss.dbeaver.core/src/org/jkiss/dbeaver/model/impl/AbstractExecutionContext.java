/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
public abstract class AbstractExecutionContext implements DBCExecutionContext, DBRBlockingObject {

    private DBRProgressMonitor monitor;
    private DBCExecutionPurpose purpose;
    private String taskTitle;
    private DBDDataFormatterProfile dataFormatterProfile;

    public AbstractExecutionContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        this.monitor = monitor;
        this.purpose = purpose;
        this.taskTitle = taskTitle;

        if (taskTitle != null) {
            monitor.startBlock(this, taskTitle);
        }

        QMUtils.getDefaultHandler().handleContextOpen(this);
    }

    public String getTaskTitle()
    {
        return taskTitle;
    }

    public boolean isConnected()
    {
        return true;
    }

    public DBRProgressMonitor getProgressMonitor()
    {
        return monitor;
    }

    public DBCTransactionManager getTransactionManager()
    {
        return new AbstractTransactionManager();
    }

    public DBCExecutionPurpose getPurpose()
    {
        return purpose;
    }

    public DBDDataFormatterProfile getDataFormatterProfile()
    {
        if (dataFormatterProfile == null) {
            return getDataSource().getContainer().getDataFormatterProfile();
        }
        return dataFormatterProfile;
    }

    public void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        dataFormatterProfile = formatterProfile;
    }

    public DBDValueHandler getDefaultValueHandler()
    {
        return DBCDefaultValueHandler.INSTANCE;
    }

    public void close()
    {
        QMUtils.getDefaultHandler().handleContextClose(this);
    }

    protected class AbstractTransactionManager implements DBCTransactionManager {

        public DBPDataSource getDataSource()
        {
            return AbstractExecutionContext.this.getDataSource();
        }

        public DBPTransactionIsolation getTransactionIsolation()
            throws DBCException
        {
            return null;
        }

        public void setTransactionIsolation(DBPTransactionIsolation transactionIsolation)
            throws DBCException
        {
            throw new DBCException("Transaction isolation change not supported");
        }

        public boolean isAutoCommit()
            throws DBCException
        {
            return true;
        }

        public void setAutoCommit(boolean autoCommit)
            throws DBCException
        {
            throw new DBCException("Auto-commit change not supported");
        }

        public boolean supportsSavepoints()
        {
            return getDataSource().getInfo().supportsSavepoints();
        }

        public DBCSavepoint setSavepoint(String name)
            throws DBCException
        {
            throw new DBCException("Savepoint not supported");
        }

        public void releaseSavepoint(DBCSavepoint savepoint)
            throws DBCException
        {
            throw new DBCException("Savepoint not supported");
        }

        public void commit()
            throws DBCException
        {
            // do nothing
        }

        public void rollback(DBCSavepoint savepoint)
            throws DBCException
        {
            throw new DBCException("Transactions not supported");
        }
    }

}
