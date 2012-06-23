/*
 * Copyright (C) 2010-2012 Serge Rieder
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

    @Override
    public boolean belongsTo(Object family)
    {
        return getDataSource() == family || family == DBPDataSource.class;
    }


}
