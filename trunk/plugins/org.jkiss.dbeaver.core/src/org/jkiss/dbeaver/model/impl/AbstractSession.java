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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
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
    private boolean holdsBlock = false;
    private boolean loggingEnabled = true;

    public AbstractSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        this.monitor = monitor;
        this.purpose = purpose;
        this.taskTitle = taskTitle;

        if (taskTitle != null) {
            monitor.startBlock(this, taskTitle);
            holdsBlock = true;
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
    public DBCExecutionPurpose getPurpose()
    {
        return purpose;
    }

    @Override
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    @Override
    public void enableLogging(boolean enable) {
        loggingEnabled = enable;
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

    @NotNull
    @Override
    public DBDValueHandler getDefaultValueHandler()
    {
        return DefaultValueHandler.INSTANCE;
    }

    @Override
    public void close()
    {
        if (holdsBlock) {
            monitor.endBlock();
            holdsBlock = false;
        }
        QMUtils.getDefaultHandler().handleSessionClose(this);
    }

}
