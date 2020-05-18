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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;

import java.util.ArrayList;
import java.util.List;

/**
 * Invalidate datasource job.
 * Invalidates all datasource contexts (not just the one passed in constructor).
 */
public class InvalidateJob extends DataSourceJob
{
    private static final Log log = Log.getLog(InvalidateJob.class);

    private static final String TASK_INVALIDATE = "dsInvalidate";

    public static class ContextInvalidateResult {
        public final DBCExecutionContext.InvalidateResult result;
        public final Exception error;

        ContextInvalidateResult(DBCExecutionContext.InvalidateResult result, Exception error) {
            this.result = result;
            this.error = error;
        }

        @Override
        public String toString() {
            return result.name();
        }
    }

    private long timeSpent;
    private List<ContextInvalidateResult> invalidateResults = new ArrayList<>();
    private Runnable feedbackHandler;

    public InvalidateJob(
        DBPDataSource dataSource)
    {
        super("Invalidate " + dataSource.getContainer().getName(), DBUtils.getDefaultContext(dataSource.getDefaultInstance(), false));
    }

    public List<ContextInvalidateResult> getInvalidateResults() {
        return invalidateResults;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public Runnable getFeedbackHandler() {
        return feedbackHandler;
    }

    public void setFeedbackHandler(Runnable feedbackHandler) {
        this.feedbackHandler = feedbackHandler;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        DBPDataSource dataSource = getExecutionContext().getDataSource();

        // Disable disconnect on failure. It is the worst case anyway.
        // Not sure that we should force disconnect even here.
        this.invalidateResults = invalidateDataSource(monitor, dataSource, false, true, feedbackHandler);

        return Status.OK_STATUS;
    }

    public static List<ContextInvalidateResult> invalidateDataSource(DBRProgressMonitor monitor, DBPDataSource dataSource, boolean disconnectOnFailure, boolean showErrors, Runnable feedback) {
        List<ContextInvalidateResult> invalidateResults = new ArrayList<>();

        DBPDataSourceContainer container = dataSource.getContainer();

        boolean networkOK;
        int goodContextsNumber, aliveContextsNumber;

        monitor.beginTask("Invalidate datasource '" + dataSource.getContainer().getName() + "'", 1);

        monitor.subTask("Obtain exclusive datasource lock");
        Object dsLock = container.getExclusiveLock().acquireTaskLock(TASK_INVALIDATE, true);
        if (dsLock == DBPExclusiveResource.TASK_PROCESED) {
            // Already invalidated
            monitor.done();
            log.debug("Datasource was already invalidated");
            return invalidateResults;
        }
        try {
            long timeSpent = 0;

            monitor.subTask("Invalidate network connection");
            DBWNetworkHandler[] activeHandlers = container.getActiveNetworkHandlers();
            networkOK = true;
            aliveContextsNumber = 0;
            goodContextsNumber = 0;
            if (activeHandlers != null && activeHandlers.length > 0) {
                for (DBWNetworkHandler nh : activeHandlers) {
                    monitor.subTask("Invalidate handler [" + container.getClass().getSimpleName() + "]");
                    try {
                        nh.invalidateHandler(monitor, dataSource);
                    } catch (Exception e) {
                        invalidateResults.add(new ContextInvalidateResult(DBCExecutionContext.InvalidateResult.ERROR, e));
                        networkOK = false;
                        break;
                    }
                }
            }

            // Invalidate datasource
            int totalContexts = 0;
            monitor.subTask("Invalidate connections of [" + container.getName() + "]");
            for (DBSInstance instance : dataSource.getAvailableInstances()) {
                for (DBCExecutionContext context : instance.getAllContexts()) {
                    totalContexts++;
                    if (networkOK) {
                        long startTime = System.currentTimeMillis();
                        Object exclusiveLock = instance.getExclusiveLock().acquireExclusiveLock();
                        try {
                            final DBCExecutionContext.InvalidateResult result = context.invalidateContext(monitor, disconnectOnFailure);
                            if (result != DBCExecutionContext.InvalidateResult.ERROR) {
                                goodContextsNumber++;
                            }
                            if (result == DBCExecutionContext.InvalidateResult.ALIVE) {
                                aliveContextsNumber++;
                            }
                            invalidateResults.add(new ContextInvalidateResult(result, null));
                        } catch (Exception e) {
                            invalidateResults.add(new ContextInvalidateResult(DBCExecutionContext.InvalidateResult.ERROR, e));
                        } finally {
                            timeSpent += (System.currentTimeMillis() - startTime);
                            instance.getExclusiveLock().releaseExclusiveLock(exclusiveLock);
                        }
                    }
                }
            }

            if (goodContextsNumber > 0 && goodContextsNumber == aliveContextsNumber) {
                // Nothing to reinit, all contexts are alive. Why we are here??
                return invalidateResults;
            }
            if (goodContextsNumber == 0 && disconnectOnFailure) {
                // Close whole datasource. Target host seems to be unavailable
                try {
                    container.disconnect(monitor);
                } catch (Exception e) {
                    log.error("Error closing inaccessible datasource", e);
                }
                StringBuilder msg = new StringBuilder();
                for (ContextInvalidateResult result : invalidateResults) {
                    if (result.error != null) {
                        if (msg.length() > 0) msg.append("\n");
                        msg.append(result.error.getMessage());
                    }
                }
                DBWorkbench.getPlatformUI().showError("Forced disconnect", "Datasource '" + container.getName() + "' was disconnected: destination database unreachable.\n" + msg);
            }

            if (totalContexts > 0) {
                if (goodContextsNumber == 0) {
                    if (showErrors) {
                        DBeaverNotifications.showNotification(
                            dataSource,
                            DBeaverNotifications.NT_RECONNECT,
                            "Datasource invalidate failed",
                            DBPMessageType.ERROR,
                            feedback);
                    }
                } else {
                    DBeaverNotifications.showNotification(
                        dataSource,
                        DBeaverNotifications.NT_RECONNECT,
                        "Datasource was invalidated\n\n" +
                            "Live connection count: " + goodContextsNumber + "/" + totalContexts,
                        DBPMessageType.INFORMATION);
                }
            }
        } finally {
            container.getExclusiveLock().releaseTaskLock(TASK_INVALIDATE, dsLock);
            monitor.done();
        }

        return invalidateResults;
    }

    public static void invalidateTransaction(DBRProgressMonitor monitor, DBPDataSource dataSource, DBCExecutionContext executionContext) {
        // Invalidate transactions
        if (executionContext != null) {
            monitor.subTask("Invalidate context [" + executionContext.getDataSource().getContainer().getName() + "/" + executionContext.getContextName() + "] transactions");
            invalidateTransaction(monitor, executionContext);
        } else {
            monitor.subTask("Invalidate datasource [" + dataSource.getContainer().getName() + "] transactions");
            for (DBSInstance instance : dataSource.getAvailableInstances()) {
                for (DBCExecutionContext context : instance.getAllContexts()) {
                    invalidateTransaction(monitor, context);
                }
            }
        }
    }

    public static void invalidateTransaction(DBRProgressMonitor monitor, DBCExecutionContext context) {
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
        if (txnManager != null) {
            try {
                if (!txnManager.isAutoCommit()) {
                    try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback failed transaction")) {
                        txnManager.rollback(session, null);
                    }
                }
            } catch (DBCException e) {
                log.error("Error invalidating aborted transaction", e);
            }
        }
    }


    @Override
    protected void canceling()
    {
        getThread().interrupt();
    }

}