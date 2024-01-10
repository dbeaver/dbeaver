/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.HashSet;
import java.util.Set;

/**
 * EndIdleTransactionsJob
 */
abstract class DataSourceUpdaterJob extends AbstractJob {

    private static final Set<String> activeDataSources = new HashSet<>();

    public static boolean isInProcess(DBPDataSourceContainer ds) {
        synchronized (activeDataSources) {
            return activeDataSources.contains(ds.getId());
        }
    }

    public DataSourceUpdaterJob(String name) {
        super(name);
    }

    public abstract DBPDataSource getDataSource();

    protected abstract IStatus updateDataSource(DBRProgressMonitor monitor);

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        if (getDataSource() == null) {
            return Status.CANCEL_STATUS;
        }
        String dsId = getDataSource().getContainer().getId();
        synchronized (activeDataSources) {
            if (activeDataSources.contains(dsId)) {
                return Status.CANCEL_STATUS;
            }
            activeDataSources.add(dsId);
        }
        try {
            return updateDataSource(monitor);
        } finally {
            synchronized (activeDataSources) {
                activeDataSources.remove(dsId);
            }
        }
    }

}
