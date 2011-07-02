/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * ConnectJob
 */
public class ConnectJob extends AbstractJob
{
    static final Log log = LogFactory.getLog(ConnectJob.class);

    private DataSourceDescriptor container;
    private volatile Thread connectThread;

    public ConnectJob(
        DataSourceDescriptor container)
    {
        super("Connect to " + container.getName());
        setUser(true);
        setProperty(IProgressConstants.ICON_PROPERTY, ImageDescriptor.createFromImage(container.getDriver().getIcon()));
        this.container = container;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            connectThread = getThread();
            String oldName = connectThread.getName();
            connectThread.setName("Connect to datasource '" + container.getName() + "'");

            try {
                container.connect(monitor);
            } finally {
                connectThread.setName(oldName);
                connectThread = null;
            }

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Connected");
        }
        catch (Throwable ex) {
            log.debug(ex);
            return RuntimeUtils.makeExceptionStatus(
                "Error connecting to datasource '" + container.getName() + "'",
                ex);
        }
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