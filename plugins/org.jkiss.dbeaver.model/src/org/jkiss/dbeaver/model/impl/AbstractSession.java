/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
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

    @NotNull
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

    @NotNull
    @Override
    public DBRProgressMonitor getProgressMonitor()
    {
        return monitor;
    }

    @NotNull
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
