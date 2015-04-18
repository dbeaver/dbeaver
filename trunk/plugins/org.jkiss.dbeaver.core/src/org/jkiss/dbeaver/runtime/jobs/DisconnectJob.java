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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Disconnect job
 */
public class DisconnectJob extends EventProcessorJob
{

    public DisconnectJob(DataSourceDescriptor container)
    {
        super(NLS.bind(CoreMessages.runtime_jobs_disconnect_name, container.getName()), container);
        setUser(true);
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            if (container.closeActiveTransaction(monitor)) {
                processEvents(DBPConnectionEventType.BEFORE_DISCONNECT);
                container.disconnect(monitor);
                processEvents(DBPConnectionEventType.AFTER_DISCONNECT);
            }

        }
        catch (Exception ex) {
            UIUtils.showErrorDialog(
                null,
                NLS.bind(CoreMessages.runtime_jobs_disconnect_error, container.getName()),
                null,
                ex);
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