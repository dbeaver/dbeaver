/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
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
        if (loggingEnabled) {
            QMUtils.getDefaultHandler().handleSessionOpen(this);
        }
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
        if (loggingEnabled) {
            QMUtils.getDefaultHandler().handleSessionClose(this);
        }
    }

}
