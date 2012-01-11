/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.text.MessageFormat;

/**
 * ConnectJob
 */
public class ConnectJob extends EventProcessorJob
{
    static final Log log = LogFactory.getLog(ConnectJob.class);

    private volatile Thread connectThread;

    public ConnectJob(
        DataSourceDescriptor container)
    {
        super(NLS.bind(CoreMessages.runtime_jobs_connect_name, container.getName()), container);
        setUser(true);
        setProperty(IProgressConstants.ICON_PROPERTY, ImageDescriptor.createFromImage(container.getDriver().getIcon()));
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            connectThread = getThread();
            String oldName = connectThread.getName();
            if (connectThread != null) {
                connectThread.setName(NLS.bind(CoreMessages.runtime_jobs_connect_thread_name, container.getName()));
            }

            processEvents(DBPConnectionEventType.BEFORE_CONNECT);

            try {
                container.connect(monitor);
            } finally {
                if (connectThread != null) {
                    connectThread.setName(oldName);
                    connectThread = null;
                }
            }

            processEvents(DBPConnectionEventType.AFTER_CONNECT);

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                CoreMessages.runtime_jobs_connect_status_connected);
        }
        catch (Throwable ex) {
            log.debug(ex);
            return RuntimeUtils.makeExceptionStatus(
                NLS.bind(CoreMessages.runtime_jobs_connect_status_error, container.getName()),
                ex);
        }
    }

    public IStatus runSync(DBRProgressMonitor monitor)
    {
        setThread(Thread.currentThread());
        return run(monitor);
    }

    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    protected void canceling()
    {
        if (connectThread != null) {
            connectThread.interrupt();
        }
    }

}