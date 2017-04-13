/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * Connect job.
 * Always returns OK status.
 * To get real status use getConectStatus.
 */
public class ConnectJob extends EventProcessorJob
{
    private volatile Thread connectThread;
    protected boolean initialize = true;
    protected boolean reflect = true;
    protected Throwable connectError;
    protected IStatus connectStatus;

    public ConnectJob(
        DataSourceDescriptor container)
    {
        super(NLS.bind(CoreMessages.runtime_jobs_connect_name, container.getName()), container);
        setUser(true);
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
            String oldName = connectThread.getName();
            if (reflect) {
                connectThread.setName(NLS.bind(CoreMessages.runtime_jobs_connect_thread_name, container.getName()));
            }

            processEvents(DBPConnectionEventType.BEFORE_CONNECT);

            try {
                container.connect(monitor, initialize, reflect);
            } finally {
                if (connectThread != null) {
                    connectThread.setName(oldName);
                    connectThread = null;
                }
            }

            processEvents(DBPConnectionEventType.AFTER_CONNECT);

            connectStatus = Status.OK_STATUS;
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