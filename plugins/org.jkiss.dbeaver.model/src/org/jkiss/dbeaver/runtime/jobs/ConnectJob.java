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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * Connect job.
 * Always returns OK status.
 * To get real status use getConectStatus.
 */
public class ConnectJob extends AbstractJob
{
    private static final Log log = Log.getLog(ConnectJob.class);

    private volatile Thread connectThread;
    protected boolean initialize = true;
    protected boolean reflect = true;
    protected Throwable connectError;
    protected IStatus connectStatus;
    protected final DBPDataSourceContainer container;

    public ConnectJob(
        DBPDataSourceContainer container)
    {
        super("Connect to '" + container.getName() + "'");
        setUser(true);
        this.container = container;
    }

    public IStatus getConnectStatus() {
        return connectStatus;
    }

    public Throwable getConnectError() {
        return connectError;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            connectThread = getThread();
            String oldName = connectThread == null ? null : connectThread.getName();
            if (reflect && connectThread != null) {
                connectThread.setName(getName());
            }

            try {
                final boolean connected = container.connect(monitor, initialize, reflect);

                connectStatus = connected ? Status.OK_STATUS : Status.CANCEL_STATUS;
            } finally {
                if (connectThread != null && oldName != null) {
                    connectThread.setName(oldName);
                    connectThread = null;
                }
            }
        }
        catch (Throwable ex) {
            log.debug(ex);
            connectError = ex;
            connectStatus = GeneralUtils.makeExceptionStatus(ex);
        }

        return Status.OK_STATUS;
    }

    public IStatus runSync(DBRProgressMonitor monitor)
    {
        AbstractJob curJob = CURRENT_JOB.get();
        if (curJob != null) {
            curJob.setAttachedJob(this);
        }
        try {
            setThread(Thread.currentThread());
            reflect = false;
            return run(monitor);
        } finally {
            if (curJob != null) {
                curJob.setAttachedJob(null);
            }
        }
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    @Override
    protected void canceling()
    {
        if (connectThread != null) {
            connectThread.interrupt();
        }
    }

}