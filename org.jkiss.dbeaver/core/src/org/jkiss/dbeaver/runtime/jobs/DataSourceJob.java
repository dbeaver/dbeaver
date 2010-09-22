/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.AbstractJob;

/**
 * DataSourceJob
 */
public abstract class DataSourceJob extends AbstractJob implements DBPDataSourceUser
{
    private DBSDataSourceContainer dataSourceContainer;

    protected DataSourceJob(String name, ImageDescriptor image, DBPDataSource dataSource)
    {
        this(name, image, dataSource.getContainer());
    }

    protected DataSourceJob(String name, ImageDescriptor image, DBSDataSourceContainer dataSourceContainer)
    {
        super(name);
        this.dataSourceContainer = dataSourceContainer;

        setUser(true);
        //setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
        //setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);
        if (image != null) {
            setProperty(IProgressConstants.ICON_PROPERTY, image);
        }
    }

    public DBPDataSource getDataSource()
    {
        return dataSourceContainer.getDataSource();
    }

    protected void startJob()
    {
        dataSourceContainer.acquire(this);
    }

    protected void endJob()
    {
        dataSourceContainer.release(this);
    }

    public boolean needsConnection()
    {
        return true;
    }

    public boolean belongsTo(Object family)
    {
        return getDataSource() == family || family == DBPDataSource.class;
    }

}
