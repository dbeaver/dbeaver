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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Invalidate datasource job.
 * Invalidates all datasource contexts (not just the one passed in constructor).
 */
public class InvalidateJob extends DataSourceJob
{
    private static final Log log = Log.getLog(InvalidateJob.class);

    private static final String TASK_INVALIDATE = "dsInvalidate";

    public static class ContextInvalidateResult {

        private final DBPDataSource dataSource;
        private final Exception exception;

        public ContextInvalidateResult(DBPDataSource dataSource) {
            this.dataSource = dataSource;
            this.exception = null;
        }

        public ContextInvalidateResult(DBPDataSource dataSource, Exception exception) {
            this.dataSource = dataSource;
            this.exception = exception;
        }

        public static ContextInvalidateResult newSuccess(@NotNull DBPDataSource dataSource) {
            return new ContextInvalidateResult(dataSource);
        }

        public static ContextInvalidateResult newError(@NotNull DBPDataSource dataSource, @NotNull Exception exception) {
            return new ContextInvalidateResult(dataSource, exception);
        }

        public DBPDataSource getDataSource() {
            return dataSource;
        }

        public Exception getException() {
            return exception;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public boolean isError() {
            return exception != null;
        }
    }

    public interface InvalidationFeedbackHandler {
        boolean confirmInvalidate(@NotNull Set<DBPDataSourceContainer> containersToInvalidate);

        void onInvalidateSuccess(@NotNull DBPDataSourceContainer container, @NotNull Collection<ContextInvalidateResult> results);

        void onInvalidateFailure(@NotNull DBPDataSourceContainer container, @NotNull Collection<ContextInvalidateResult> results);
    }

    private List<ContextInvalidateResult> invalidateResults = new ArrayList<>();
    private InvalidationFeedbackHandler feedbackHandler;

    public InvalidateJob(
        DBPDataSource dataSource)
    {
        super("Invalidate " + dataSource.getContainer().getName(), DBUtils.getDefaultContext(dataSource.getDefaultInstance(), false));
    }

    public List<ContextInvalidateResult> getInvalidateResults() {
        return invalidateResults;
    }

    public void setFeedbackHandler(@Nullable InvalidationFeedbackHandler feedbackHandler) {
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
        @Nullable InvalidationFeedbackHandler feedbackHandler
    ) {
        final Set<DBPDataSourceContainer> containers = new LinkedHashSet<>();
        collectDependentDataSources(dataSource, containers);

        final Map<DBPDataSourceContainer, Object> locks = new HashMap<>();
        for (var it = containers.iterator(); it.hasNext(); ) {
            final DBPDataSourceContainer container = it.next();
            monitor.subTask("Obtain exclusive datasource lock for '" + container.getName() + "'");

            final Object lock = container.getExclusiveLock().acquireTaskLock(TASK_INVALIDATE, true);
            if (lock == DBPExclusiveResource.TASK_PROCESED) {
                log.debug("Datasource '" + container.getName() + "' was already invalidated");
                it.remove();
            } else {
                locks.put(container, lock);
            }
        }

        if (locks.isEmpty()) {
            log.debug("Nothing to invalidate");
            monitor.done();
            return List.of();
        }

        try {
            if (feedbackHandler == null || feedbackHandler.confirmInvalidate(containers)) {
                return invalidateDataSources0(monitor, containers, disconnectOnFailure, showErrors, feedbackHandler);
            } else {
                log.debug("Invalidate cancelled by user");
                return List.of();
            }
        } finally {
            monitor.subTask("Release exclusive datasource locks");
            for (Map.Entry<DBPDataSourceContainer, Object> entry : locks.entrySet()) {
                entry.getKey().getExclusiveLock().releaseTaskLock(TASK_INVALIDATE, entry.getValue());
            }

            monitor.done();
        }
    }

    @NotNull
    private static List<ContextInvalidateResult> invalidateDataSources0(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Set<DBPDataSourceContainer> containers,
        boolean disconnectOnFailure,
        boolean showErrors,
        @Nullable InvalidationFeedbackHandler feedbackHandler
    ) {
        monitor.beginTask("Invalidate data sources", containers.size());

        final List<ContextInvalidateResult> finalResults = new ArrayList<>();

        for (DBCInvalidatePhase phase : DBCInvalidatePhase.values()) {
            finalResults.clear();

            enum Severity {
                FINE,
                SEVERE
            }

            final Map<DBPDataSourceContainer, Severity> failed = new HashMap<>();

            for (DBPDataSourceContainer container : containers) {
                if (failed.get(container) == Severity.SEVERE) {
                    continue;
                }

                DBPDataSource dataSource = container.getDataSource();
                if (dataSource == null) {
                    continue;
                }

                var results = invalidateNetworkHandlers(monitor, dataSource, phase);

                if (anyFailed(results)) {
                    failed.put(container, Severity.SEVERE);
                }

                finalResults.addAll(results);
            }

            for (DBPDataSourceContainer container : containers) {
                if (failed.containsKey(container)) {
                    continue;
                }

                DBPDataSource dataSource = container.getDataSource();
                if (dataSource == null) {
                    continue;
                }
                var results = invalidateInstances(monitor, dataSource, phase);

                if (anyFailed(results)) {
                    failed.put(container, Severity.FINE);
                }

                if (disconnectOnFailure && allFailed(results)) {
                    // Close whole datasource. Target host seems to be unavailable
                    try {
                        container.disconnect(monitor);
                    } catch (Exception e) {
                        log.error("Error closing inaccessible datasource", e);
                    }
                    final String errors = results.stream()
                        .filter(ContextInvalidateResult::isError)
                        .map(result -> result.getException().getMessage())
                        .collect(Collectors.joining("\n"));
                    DBWorkbench.getPlatformUI().showError(
                        "Forced disconnect",
                        "Datasource '" + container.getName() + "' was disconnected: destination database unreachable.\n" + errors
                    );
                }

                if (phase == DBCInvalidatePhase.AFTER_INVALIDATE) {
                    if (feedbackHandler != null) {
                        if (anySucceeded(results)) {
                            feedbackHandler.onInvalidateSuccess(container, results);
                        } else if (showErrors) {
                            feedbackHandler.onInvalidateFailure(container, results);
                        }
                    }

                    monitor.worked(1);
                }

                finalResults.addAll(results);
            }
        }

        monitor.done();

        return finalResults;
    }

    private static void collectDependentDataSources(
        @NotNull DBPDataSource dataSource,
        @NotNull Set<DBPDataSourceContainer> result
    ) {
        final Deque<DBPDataSourceContainer> pending = new ArrayDeque<>();
        pending.add(dataSource.getContainer());

        while (!pending.isEmpty()) {
            final DBPDataSourceContainer container = pending.remove();
            if (!result.add(container)) {
                continue;
            }
            for (DBWNetworkHandler handler : container.getActiveNetworkHandlers()) {
                final DBPDataSourceContainer[] dataSources = handler.getDependentDataSources();
                for (DBPDataSourceContainer other : dataSources) {
                    pending.offer(other);
                }
            }
        }
    }

    @NotNull
    private static Collection<ContextInvalidateResult> invalidateNetworkHandlers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCInvalidatePhase phase
    ) {
        final List<ContextInvalidateResult> results = new ArrayList<>();

        monitor.subTask("Invalidate network connection");

        for (DBWNetworkHandler nh : dataSource.getContainer().getActiveNetworkHandlers()) {
            log.debug("\tInvalidate network handler '" + nh.getClass().getSimpleName() + "' for " + dataSource.getContainer().getId());
            monitor.subTask("Invalidate handler [" + nh.getClass().getSimpleName() + "]");

            try {
                nh.invalidateHandler(monitor, dataSource, phase);
                results.add(ContextInvalidateResult.newSuccess(dataSource));
            } catch (Exception e) {
                results.add(ContextInvalidateResult.newError(dataSource, e));
            }
        }

        return results;
    }

    @NotNull
    private static Collection<ContextInvalidateResult> invalidateInstances(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCInvalidatePhase phase
    ) {
        final List<ContextInvalidateResult> results = new ArrayList<>();

        monitor.subTask("Invalidate connections of " + dataSource.getContainer().getName());

        for (DBSInstance instance : dataSource.getAvailableInstances()) {
            for (DBCExecutionContext context : instance.getAllContexts()) {
                log.debug("\tInvalidate context '" + context.getContextName()
                    + "' for " + dataSource.getContainer().getId() + " (" + phase + ")");

                final Object exclusiveLock = instance.getExclusiveLock().acquireExclusiveLock();
                try {
                    context.invalidateContext(monitor, phase);
                    results.add(ContextInvalidateResult.newSuccess(dataSource));
                } catch (Exception e) {
                    log.debug("\tFailed: " + e.getMessage());
                    results.add(ContextInvalidateResult.newError(dataSource, e));
                } finally {
                    instance.getExclusiveLock().releaseExclusiveLock(exclusiveLock);
                }
            }
        }

        return results;
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

    public static boolean allSucceeded(@NotNull Collection<ContextInvalidateResult> results) {
        return results.stream().allMatch(ContextInvalidateResult::isSuccess);
    }

    public static boolean anySucceeded(@NotNull Collection<ContextInvalidateResult> results) {
        return results.stream().anyMatch(ContextInvalidateResult::isSuccess);
    }

    public static boolean allFailed(@NotNull Collection<ContextInvalidateResult> results) {
        return results.stream().allMatch(ContextInvalidateResult::isError);
    }

    public static boolean anyFailed(@NotNull Collection<ContextInvalidateResult> results) {
        return results.stream().anyMatch(ContextInvalidateResult::isError);
    }

    public static int getSucceededCount(@NotNull Collection<ContextInvalidateResult> results) {
        return (int) results.stream().filter(ContextInvalidateResult::isSuccess).count();
    }

    @Override
    protected void canceling()
    {
        getThread().interrupt();
    }

}