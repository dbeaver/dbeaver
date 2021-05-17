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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KeepAlivePingJob
 */
class KeepAlivePingJob extends AbstractJob {
    private static final Log log = Log.getLog(KeepAlivePingJob.class);

    private static final Map<String, Integer> failedAttempts = new HashMap<>();

    private final DBPDataSource dataSource;
    private final boolean disconnectOnError;

    KeepAlivePingJob(DBPDataSource dataSource, boolean disconnectOnError) {
        super("Connection ping (" + dataSource.getContainer().getName() + ")");
        setUser(false);
        setSystem(true);
        this.dataSource = dataSource;
        this.disconnectOnError = disconnectOnError;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        boolean hasDeadContexts = false;
        for (final DBSInstance instance : dataSource.getAvailableInstances()) {
            for (final DBCExecutionContext context : instance.getAllContexts()) {
                try {
                    context.checkContextAlive(monitor);
                } catch (Exception e) {
                    log.debug("Context [" + dataSource.getName() + "::" + context.getContextName() + "] check failed: " + e.getMessage());
                    hasDeadContexts = true;
                    break;
                }
            }
        }
        if (hasDeadContexts) {
            // Invalidate whole datasource. Do not log errors (as it can spam tons of logs)
            final List<InvalidateJob.ContextInvalidateResult> results = InvalidateJob.invalidateDataSource(
                monitor,
                dataSource,
                disconnectOnError,
                false,
                () -> DBWorkbench.getPlatformUI().openConnectionEditor(dataSource.getContainer()));
            synchronized (failedAttempts) {
                String dsId = dataSource.getContainer().getId();
                if (isSuccess(results) || disconnectOnError) {
                    log.debug("Datasource " + dataSource.getName() + " invalidated: " + results);
                    failedAttempts.remove(dsId);
                } else {
                    log.debug("Datasource " + dataSource.getName() + " invalidate failed: " + results);
                    Integer curAttempts = failedAttempts.get(dsId);
                    if (curAttempts == null) {
                        curAttempts = 1;
                    } else {
                        curAttempts++;
                    }
                    failedAttempts.put(dsId, curAttempts);
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

    public static int getFailedAttemptCount(DBPDataSource dataSource) {
        synchronized (failedAttempts) {
            Integer attempts = failedAttempts.get(dataSource.getContainer().getId());
            return attempts == null ? 0 : attempts;
        }
    }

}