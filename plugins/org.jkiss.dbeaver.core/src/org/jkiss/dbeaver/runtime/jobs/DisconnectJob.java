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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * Disconnect job.
 * Always returns OK status.
 * To get real status use getConectStatus.
 */
public class DisconnectJob extends EventProcessorJob
{
    private IStatus connectStatus;

    public DisconnectJob(DBPDataSourceContainer container)
    {
        super(NLS.bind(CoreMessages.runtime_jobs_disconnect_name, container.getName()), container);
        setUser(true);
    }

    public IStatus getConnectStatus() {
        return connectStatus;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            processEvents(DBPConnectionEventType.BEFORE_DISCONNECT);
            container.disconnect(monitor);
            processEvents(DBPConnectionEventType.AFTER_DISCONNECT);

            connectStatus = Status.OK_STATUS;
        }
        catch (Throwable ex) {
            connectStatus = GeneralUtils.makeExceptionStatus(ex);
        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    @Override
    protected void canceling()
    {
        getThread().interrupt();
    }

}