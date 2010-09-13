/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * ConnectJob
 */
public class ConnectJob extends AbstractJob
{
    static final Log log = LogFactory.getLog(ConnectJob.class);

    private DataSourceDescriptor container;
    private Thread connectThread;

    public ConnectJob(
        DataSourceDescriptor container)
    {
        super("Connect to " + container.getName());
        setUser(true);
        this.container = container;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            connectThread = getThread();
            connectThread.setName("Connect to datasource '" + container.getName() + "'");

            container.connect(monitor);

            connectThread = null;
            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Connected");
        }
        catch (Throwable ex) {
            log.debug(ex);
            return DBeaverUtils.makeExceptionStatus(
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