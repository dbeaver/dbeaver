/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
        if (image == null) {
            image = ImageDescriptor.createFromImage(dataSourceContainer.getDriver().getIcon());
        }
        setProperty(IProgressConstants.ICON_PROPERTY, image);

        addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void aboutToRun(IJobChangeEvent event)
            {
                DataSourceJob.this.dataSourceContainer.acquire(DataSourceJob.this);
            }
            @Override
            public void done(IJobChangeEvent event)
            {
                DataSourceJob.this.dataSourceContainer.release(DataSourceJob.this);
            }
        });
    }

    public DBPDataSource getDataSource()
    {
        return dataSourceContainer.getDataSource();
    }

    public boolean belongsTo(Object family)
    {
        return getDataSource() == family || family == DBPDataSource.class;
    }


}
