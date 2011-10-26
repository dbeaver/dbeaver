/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.text.MessageFormat;

/**
 * DisconnectJob
 */
public class DisconnectJob extends EventProcessorJob
{
    static final Log log = LogFactory.getLog(DisconnectJob.class);

    public DisconnectJob(
        DataSourceDescriptor container)
    {
        super(NLS.bind(CoreMessages.runtime_jobs_disconnect_name, container.getName()), container);
        setUser(true);
    }

    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            processEvents(DBPConnectionEventType.BEFORE_DISCONNECT);

            container.disconnect(monitor);

            processEvents(DBPConnectionEventType.AFTER_DISCONNECT);

            return Status.OK_STATUS;
        }
        catch (Exception ex) {
            return RuntimeUtils.makeExceptionStatus(
                NLS.bind(CoreMessages.runtime_jobs_disconnect_error, container.getName()),
                ex);
        }
    }

    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    protected void canceling()
    {
        getThread().interrupt();
    }

}