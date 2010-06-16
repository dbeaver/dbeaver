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
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.DBException;

/**
 * ConnectJob
 */
public class ConnectJob extends AbstractJob
{
    static Log log = LogFactory.getLog(ConnectJob.class);

    private DataSourceDescriptor container;
    private DBPDataSource dataSource;

    public ConnectJob(
        DataSourceDescriptor container)
    {
        super("Connect to " + container.getName());
        setUser(true);
        //setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
        setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);
        this.container = container;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            monitor.beginTask("Open Datasource ...", 1);
            dataSource = container.getDriver().getDataSourceProvider().openDataSource(
                monitor, container
            );
            monitor.worked(1);
            monitor.done();

            dataSource.initialize(monitor);
            // Change connection properties

            setDataSourceSettings(monitor, dataSource);

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Connected");
        }
        catch (Throwable ex) {
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
        getThread().interrupt();
    }

    private void setDataSourceSettings(DBRProgressMonitor monitor, DBPDataSource dataSource)
        throws DBException
    {
        monitor.beginTask("Set session defaults ...", 1);

        DBCSession session = dataSource.getSession(monitor, false);

        // Change autocommit state
        try {
            boolean autoCommit = session.isAutoCommit();
            boolean newAutoCommit = container.getPreferenceStore().getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT);
            if (autoCommit != newAutoCommit) {
                session.setAutoCommit(newAutoCommit);
            }
        }
        catch (DBCException e) {
            log.error("Can't set session autocommit state", e);
        }
        monitor.done();
    }

}