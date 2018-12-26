/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

/**
 * PingJob
 */
public class PingJob extends AbstractJob
{
    private static final Log log = Log.getLog(PingJob.class);

    private final DBPDataSource dataSource;

    public PingJob(DBPDataSource dataSource)
    {
        super("Connection ping");
        setUser(false);
        setSystem(true);
        this.dataSource = dataSource;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        //log.debug("Ping connection " + dataSource.getContainer().getId());
        for (final DBSInstance instance : dataSource.getAvailableInstances()) {
            for (final DBCExecutionContext context : instance.getAllContexts()) {
                try {
                    context.checkContextAlive(monitor);
                } catch (Exception e) {
                    log.debug("Context [" + dataSource.getName() + "::" + context.getContextName() + "] check failed: " + e.getMessage());
                    if (e instanceof DBException) {
                        final List<InvalidateJob.ContextInvalidateResult> results = InvalidateJob.invalidateDataSource(monitor, dataSource, false,
                            () -> DBWorkbench.getPlatformUI().openConnectionEditor(dataSource.getContainer()));
                        log.debug("Connection invalidated: " + results);
                    }
                }
            }
        }
        return Status.OK_STATUS;
    }


}