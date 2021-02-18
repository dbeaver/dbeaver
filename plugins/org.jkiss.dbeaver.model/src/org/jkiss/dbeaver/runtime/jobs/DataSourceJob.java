/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceTask;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.utils.CommonUtils;

/**
 * DataSourceJob
 */
public abstract class DataSourceJob extends AbstractJob implements DBPDataSourceTask
{
    private final DBCExecutionContext executionContext;

    protected DataSourceJob(String name, @NotNull DBCExecutionContext executionContext)
    {
        super(CommonUtils.truncateString(name, 1000)); // Trunkate just in case
        this.executionContext = executionContext;
        final DBPDataSourceContainer dataSourceContainer = executionContext.getDataSource().getContainer();

        setUser(true);

        addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void aboutToRun(IJobChangeEvent event) {
                dataSourceContainer.acquire(DataSourceJob.this);
            }

            @Override
            public void done(IJobChangeEvent event) {
                dataSourceContainer.release(DataSourceJob.this);
            }
        });
    }

    @NotNull
    public DBPDataSourceContainer getDataSourceContainer()
    {
        return executionContext.getDataSource().getContainer();
    }

    @NotNull
    public DBCExecutionContext getExecutionContext()
    {
        return executionContext;
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return executionContext == family || family == DBPDataSource.class;
    }

    @Override
    public boolean isActiveTask() {
        return getState() == RUNNING;
    }

}
