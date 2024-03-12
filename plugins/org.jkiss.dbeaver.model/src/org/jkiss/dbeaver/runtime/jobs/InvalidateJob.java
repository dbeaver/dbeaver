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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;

import java.util.*;

/**
 * Invalidate datasource job.
 * Invalidates all datasource contexts (not just the one passed in constructor).
 */
public class InvalidateJob extends DataSourceJob
{
    private static final Log log = Log.getLog(InvalidateJob.class);

    private static final String TASK_INVALIDATE = "dsInvalidate";

    public sealed interface ContextInvalidateResult {
        record Success() implements ContextInvalidateResult {
        }

        record Error(@NotNull Exception exception) implements ContextInvalidateResult {
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

    @NotNull
    public static List<ContextInvalidateResult> invalidateDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        boolean disconnectOnFailure,
        boolean showErrors,
        @Nullable  Runnable feedback
    ) {
        DBWNetworkHandler[] activeHandlers = dataSource.getContainer().getActiveNetworkHandlers();
        DBPDataSourceContainer container = dataSource.getContainer();

        final Set<DBPDataSourceContainer> dependentDataSources = new HashSet<>();
        for (DBWNetworkHandler handler : activeHandlers) {
            Collections.addAll(dependentDataSources, handler.getDependentDataSources());
        }
        if (dependentDataSources.isEmpty()) {
            dependentDataSources.add(dataSource.getContainer());
        }

        final Map<DBPDataSourceContainer, Object> locks = new HashMap<>();
        for (var it = dependentDataSources.iterator(); it.hasNext(); ) {
            final DBPDataSourceContainer currentContainer = it.next();
            monitor.subTask("Obtain exclusive datasource lock for '" + currentContainer.getName() + "'");

            final Object lock = currentContainer.getExclusiveLock().acquireTaskLock(TASK_INVALIDATE, true);
            if (lock == DBPExclusiveResource.TASK_PROCESED) {
                log.debug("Datasource '" + dataSource.getContainer().getName() + "' was already invalidated");
                it.remove();
            } else {
                locks.put(currentContainer, lock);
            }
        }

        if (locks.isEmpty()) {
            // Nothing to invalidate
            monitor.done();
            return List.of();
        }

        monitor.beginTask("Invalidate datasources", dependentDataSources.size());

        try {
            final List<ContextInvalidateResult> invalidateResults = new ArrayList<>();
            for (DBCInvalidatePhase phase : DBCInvalidatePhase.values()) {
                invalidateResults.clear();
                boolean networkOK = true;

                for (DBPDataSourceContainer currentContainer : dependentDataSources) {
                    final DBPDataSource currentDataSource = currentContainer.getDataSource();

                    monitor.beginTask("Invalidate datasource '" + currentContainer.getName() + "'", 1);
                    log.debug("Invalidate datasource '" + currentContainer.getName() + "' (" + currentContainer.getId() + ")");

                    monitor.subTask("Invalidate network connection");
                    for (DBWNetworkHandler nh : activeHandlers) {
                        log.debug("\tInvalidate network handler '" + nh.getClass().getSimpleName() + "' for " + currentContainer.getId());
                        monitor.subTask("Invalidate handler [" + nh.getClass().getSimpleName() + "]");
                        try {
                            nh.invalidateHandler(monitor, currentDataSource, phase);
                        } catch (Exception e) {
                            invalidateResults.add(new ContextInvalidateResult.Error(e));
                            networkOK = false;
                            break;
                        }
                    }

                    // Invalidate datasource
                    int totalContexts = 0;
                    int goodContextsNumber = 0;
                    monitor.subTask("Invalidate connections of [" + currentContainer.getName() + "]");
                    for (DBSInstance instance : currentDataSource.getAvailableInstances()) {
                        for (DBCExecutionContext context : instance.getAllContexts()) {
                            log.debug("\tInvalidate context '" + context.getContextName()
                                + "' for " + currentContainer.getId() + " (" + phase + ")");
                            totalContexts++;
                            if (networkOK) {
                                Object exclusiveLock = instance.getExclusiveLock().acquireExclusiveLock();
                                try {
                                    context.invalidateContext(monitor, phase);
                                    invalidateResults.add(new ContextInvalidateResult.Success());
                                    goodContextsNumber++;
                                } catch (Exception e) {
                                    log.debug("\tFailed: " + e.getMessage());
                                    invalidateResults.add(new ContextInvalidateResult.Error(e));
                                } finally {
                                    instance.getExclusiveLock().releaseExclusiveLock(exclusiveLock);
                                }
                            }
                        }
                    }

                    if (goodContextsNumber == 0 && disconnectOnFailure) {
                        // Close whole datasource. Target host seems to be unavailable
                        try {
                            currentContainer.disconnect(monitor);
                        } catch (Exception e) {
                            log.error("Error closing inaccessible datasource", e);
                        }
                        StringBuilder msg = new StringBuilder();
                        for (ContextInvalidateResult result : invalidateResults) {
                            if (result instanceof ContextInvalidateResult.Error error) {
                                if (!msg.isEmpty()) {
                                    msg.append("\n");
                                }
                                msg.append(error.exception.getMessage());
                            }
                        }
                        DBWorkbench.getPlatformUI().showError(
                            "Forced disconnect",
                            "Datasource '" + currentContainer.getName() + "' was disconnected: destination database unreachable.\n" + msg
                        );
                    }

                    if (totalContexts > 0) {
                        if (goodContextsNumber == 0) {
                            if (showErrors) {
                                DBeaverNotifications.showNotification(
                                    currentDataSource,
                                    DBeaverNotifications.NT_RECONNECT_FAILURE,
                                    "Datasource invalidate failed",
                                    DBPMessageType.ERROR,
                                    feedback);
                            }
                        } else if (phase == DBCInvalidatePhase.AFTER_INVALIDATE) {
                            DBeaverNotifications.showNotification(
                                currentDataSource,
                                DBeaverNotifications.NT_RECONNECT_SUCCESS,
                                "Datasource was invalidated\n\n" +
                                    "Live connection count: " + goodContextsNumber + "/" + totalContexts,
                                DBPMessageType.INFORMATION);
                        }
                    }

                    monitor.worked(1);

                    if (phase == DBCInvalidatePhase.BEFORE_INVALIDATE) {
                        monitor.worked(1);
                    }
                }
            }

            return invalidateResults;
        } finally {
            monitor.subTask("Release exclusive datasource locks");
            for (Map.Entry<DBPDataSourceContainer, Object> entry : locks.entrySet()) {
                entry.getKey().getExclusiveLock().releaseTaskLock(TASK_INVALIDATE, entry.getValue());
            }

            monitor.done();
        }
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
                        // Disable logging to avoid QM handlers notifications.
                        // These notifications may trigger smart commit mode during txn recover. See #9066
                        session.enableLogging(false);
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