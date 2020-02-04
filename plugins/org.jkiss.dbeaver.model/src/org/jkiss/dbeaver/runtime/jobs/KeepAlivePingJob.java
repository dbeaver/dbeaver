/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
 * KeepAlivePingJob
 */
class KeepAlivePingJob extends AbstractJob {
    private static final Log log = Log.getLog(KeepAlivePingJob.class);

    private final DBPDataSource dataSource;

    KeepAlivePingJob(DBPDataSource dataSource) {
        super("Connection ping (" + dataSource.getContainer().getName() + ")");
        setUser(false);
        setSystem(true);
        this.dataSource = dataSource;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        for (final DBSInstance instance : dataSource.getAvailableInstances()) {
            for (final DBCExecutionContext context : instance.getAllContexts()) {
                try {
                    context.checkContextAlive(monitor);
                } catch (Exception e) {
                    log.debug("Context [" + dataSource.getName() + "::" + context.getContextName() + "] check failed: " + e.getMessage());
                    // Invalidate. Do not log errors (as it can spam tons of logs)
                    if (e instanceof DBException) {
                        final List<InvalidateJob.ContextInvalidateResult> results = InvalidateJob.invalidateDataSource(
                            monitor,
                            dataSource,
                            false,
                            false,
                            () -> DBWorkbench.getPlatformUI().openConnectionEditor(dataSource.getContainer()));
                        if (isSuccess(results)) {
                            log.debug("Connection invalidated: " + results);
                        }
                    }
                }
            }
        }
        return Status.OK_STATUS;
    }

    private boolean isSuccess(List<InvalidateJob.ContextInvalidateResult> results) {
        for (InvalidateJob.ContextInvalidateResult result : results) {
            switch (result.result) {
                case ALIVE:
                case RECONNECTED:
                case CONNECTED:
                    return true;
            }
        }
        return false;
    }


}