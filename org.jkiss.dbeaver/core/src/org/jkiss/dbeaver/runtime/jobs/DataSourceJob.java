/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.runtime.AbstractJob;

/**
 * DataSourceJob
 */
public abstract class DataSourceJob extends AbstractJob implements DBPDataSourceUser
{
    private DBPDataSource dataSource;

    protected DataSourceJob(String name, ImageDescriptor image, DBPDataSource dataSource)
    {
        super(name);
        this.dataSource = dataSource;

        setUser(true);
        //setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
        //setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);
        if (image != null) {
            setProperty(IProgressConstants.ICON_PROPERTY, image);
        }
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    protected void startJob()
    {
        dataSource.getContainer().acquire(this);
    }

    protected void endJob()
    {
        dataSource.getContainer().release(this);
    }

    public boolean needsConnection()
    {
        return true;
    }

    public boolean belongsTo(Object family)
    {
        return dataSource == family || family == DBPDataSource.class;
    }

}
