/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * Disconnect job.
 * Always returns OK status.
 * To get real status use getConectStatus.
 */
public class DisconnectJob extends AbstractJob
{
    private IStatus connectStatus;
    protected final DBPDataSourceContainer container;

    public DisconnectJob(DBPDataSourceContainer container)
    {
        super("Disconnect from '" + container.getName() + "'");
        setUser(true);
        this.container = container;
    }

    public IStatus getConnectStatus() {
        return connectStatus;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            long startTime = System.currentTimeMillis();
            container.disconnect(monitor);

            connectStatus = Status.OK_STATUS;
        }
        catch (Throwable ex) {
            connectStatus = GeneralUtils.makeExceptionStatus(ex);
        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    @Override
    protected void canceling()
    {
        getThread().interrupt();
    }

}