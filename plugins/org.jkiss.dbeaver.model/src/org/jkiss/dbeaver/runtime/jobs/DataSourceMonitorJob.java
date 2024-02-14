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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMTransactionState;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.qm.meta.QMMConnectionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;

import java.util.*;
import java.util.function.Supplier;

/**
 * DataSourceMonitorJob.
 *
 * Performs connection keep-alive ping.
 * Ends idle transactions.
 */
public class DataSourceMonitorJob extends AbstractJob {
    private static final int MONITOR_INTERVAL = 3000; // once per 3 seconds
    private static final long SYSTEM_SUSPEND_INTERVAL = 30000; // 30 seconds of inactivity - most likely a system suspend

    private static final Log log = Log.getLog(DataSourceMonitorJob.class);

    private static final int MAX_FAILED_ATTEMPTS_BEFORE_DISCONNECT = 5;
    private static final int MAX_FAILED_ATTEMPTS_BEFORE_IGNORE = 10;

    private final DBPPlatform platform;
    private final Map<String, Long> checkCache = new HashMap<>();
    private final Set<String> pingCache = new HashSet<>();
    private long lastPingTime = -1;

    public DataSourceMonitorJob(DBPPlatform platform) {
        super("Keep-Alive monitor");
        setUser(false);
        setSystem(true);
        this.platform = platform;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        if (platform.isShuttingDown()) {
            return Status.OK_STATUS;
        }
        if (lastPingTime > 0 && System.currentTimeMillis() - lastPingTime > SYSTEM_SUSPEND_INTERVAL) {
            log.debug("System suspend detected! Reinitialize all remote connections.");
        }
        lastPingTime = System.currentTimeMillis();

        doJob();

        if (!platform.isShuttingDown()) {
            scheduleMonitor();
        }
        return Status.OK_STATUS;
    }

    protected void doJob() {
        final DBPWorkspace workspace = platform.getWorkspace();
        checkDataSourceAliveInWorkspace(workspace, () -> getLastUserActivityTime(lastPingTime));
    }

    protected void checkDataSourceAliveInWorkspace(DBPWorkspace workspace, Supplier<Long> supplier) {
        for (DBPProject project : workspace.getProjects()) {
            if (project.isOpen() && project.isRegistryLoaded()) {
                DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();
                for (DBPDataSourceContainer ds : dataSourceRegistry.getDataSources()) {
                    checkDataSourceAlive(ds, supplier, workspace.getActiveProject().getWorkspaceSession());
                }
            }
        }
    }

    private void checkDataSourceAlive(final DBPDataSourceContainer dataSourceDescriptor, Supplier<Long> supplier, SMSession smSession) {
        if (!dataSourceDescriptor.isConnected()) {
            return;
        }

        final String dsId = dataSourceDescriptor.getId();
        synchronized (this) {
            if (pingCache.contains(dsId)) {
                // Ping is still in progress. Hanged?
                // Anyway - just skip it
                return;
            }
        }

        // End long transactions or connections
        if (getDisconnectTimeoutSeconds(dataSourceDescriptor) > 0 || getTransactionTimeoutSeconds(dataSourceDescriptor) > 0) {
            if (endIdleTransactionOrConnection(dataSourceDescriptor, supplier, smSession)) {
                return;
            }
        }

        // Perform keep alive request
        final int keepAliveInterval = dataSourceDescriptor.getConnectionConfiguration().getKeepAliveInterval();
        if (keepAliveInterval <= 0) {
            return;
        }

        final DBPDataSource dataSource = dataSourceDescriptor.getDataSource();
        if (dataSource == null) {
            return;
        }
        Long lastCheckTime;
        synchronized (this) {
            lastCheckTime = checkCache.get(dsId);
        }
        if (lastCheckTime == null) {
            final Date connectTime = dataSourceDescriptor.getConnectTime();
            if (connectTime != null) {
                lastCheckTime = connectTime.getTime();
            }
        }
        if (lastCheckTime == null) {
            log.debug("Can't determine last check time for " + dsId);
            return;
        }
        long curTime = System.currentTimeMillis();
        if ((curTime - lastCheckTime) / 1000 > keepAliveInterval) {
            boolean disconnectOnError = false;
            int failedAttemptCount = KeepAlivePingJob.getFailedAttemptCount(dataSource);
            if (failedAttemptCount >= MAX_FAILED_ATTEMPTS_BEFORE_IGNORE) {
                return;
            }
            if (failedAttemptCount > MAX_FAILED_ATTEMPTS_BEFORE_DISCONNECT) {
                disconnectOnError = true;
            }
            final KeepAlivePingJob pingJob = new KeepAlivePingJob(dataSource, disconnectOnError);
            pingJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    synchronized (DataSourceMonitorJob.this) {
                        checkCache.put(dsId, System.currentTimeMillis());
                        pingCache.remove(dsId);
                    }
                }
            });
            synchronized (this) {
                pingCache.add(dsId);
            }
            pingJob.schedule();
        }
    }

    private boolean endIdleTransactionOrConnection(DBPDataSourceContainer dsDescriptor, Supplier<Long> supplier, SMSession smSession) {
        if (!dsDescriptor.isConnected()) {
            return false;
        }

        final long lastUserActivityTime = supplier.get();
        if (lastUserActivityTime < 0) {
            return false;
        }

        final long idleInterval = (System.currentTimeMillis() - lastUserActivityTime) / 1000;
        final long disconnectTimeoutSeconds = getDisconnectTimeoutSeconds(dsDescriptor);
        final long rollbackTimeoutSeconds = getTransactionTimeoutSeconds(dsDescriptor);

        DBPDataSource dataSource = dsDescriptor.getDataSource();

        if (dataSource != null && disconnectTimeoutSeconds > 0 && idleInterval > disconnectTimeoutSeconds) {
            if (DisconnectJob.isInProcess(dsDescriptor)) {
                return false;
            }
            if (isExecutionInProgress(dataSource)) {
                return false;
            }

            // Kill idle connection
            DisconnectJob disconnectJob = new DisconnectJob(dsDescriptor);
            disconnectJob.schedule();

            showNotification(dataSource, dsDescriptor, smSession);
            return true;
        }

        if (rollbackTimeoutSeconds <= 0 || idleInterval < rollbackTimeoutSeconds) {
            return false;
        }
        if (EndIdleTransactionsJob.isInProcess(dsDescriptor)) {
            return false;
        }

        if (dataSource != null) {
            try {
                Map<DBCExecutionContext, DBCTransactionManager> txnToEnd = new IdentityHashMap<>();
                for (DBSInstance instance : dataSource.getAvailableInstances()) {
                    for (DBCExecutionContext ec : instance.getAllContexts()) {
                        if (ec.isConnected()) {
                            DBCTransactionManager txnManager = DBUtils.getTransactionManager(ec);
                            if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
                                QMTransactionState txnState = QMUtils.getTransactionState(ec);
                                if (txnState.getUpdateCount() > 0 && txnState.getTransactionStartTime() <= lastUserActivityTime) {
                                    txnToEnd.put(ec, txnManager);
                                }
                            }
                        }
                    }
                }

                if (!txnToEnd.isEmpty()) {
                    new EndIdleTransactionsJob(dataSource, txnToEnd).schedule();
                }
            } catch (DBCException e) {
                log.error(e);
            }
            return true;
        }

        return false;
    }

    private static boolean isExecutionInProgress(DBPDataSource dataSource) {
        for (DBSInstance instance : dataSource.getAvailableInstances()) {
            for (DBCExecutionContext context : instance.getAllContexts()) {
                QMMConnectionInfo qmConnection = QMUtils.getCurrentConnection(context);
                if (qmConnection != null) {
                    QMMStatementExecuteInfo lastExec = qmConnection.getExecutionStack();
                    if (lastExec != null && !lastExec.isClosed()) {
                        // It is in progress
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void scheduleMonitor() {
        schedule(MONITOR_INTERVAL);
    }

    public static long getDisconnectTimeoutSeconds(@NotNull DBPDataSourceContainer container) {
        DBPConnectionConfiguration config = container.getConnectionConfiguration();
        if (!config.isCloseIdleConnection()) {
            return 0;
        }
        final int timeout = config.getCloseIdleInterval();
        if (timeout > 0) {
            return timeout;
        }
        final DBPConnectionType connectionType = config.getConnectionType();
        if (connectionType.isAutoCloseConnections()) {
            return connectionType.getCloseIdleConnectionPeriod();
        }
        return 0;
    }

    public static long getTransactionTimeoutSeconds(@NotNull DBPDataSourceContainer container) {
        final DBPPreferenceStore pref = container.getPreferenceStore();
        final DBPConnectionConfiguration config = container.getConnectionConfiguration();
        long ttlSeconds = 0;

        if (pref.contains(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED)) {
            // First check datasource settings from the "Transactions" preference page
            ttlSeconds = pref.getLong(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL);
        }

        if (ttlSeconds == 0) {
            // Or get this info from the current connection type
            final DBPConnectionType connectionType = config.getConnectionType();
            if (connectionType.isAutoCloseTransactions()) {
                ttlSeconds = connectionType.getCloseIdleTransactionPeriod();
            }
        }

        return Math.max(0, ttlSeconds);
    }

    public long getLastUserActivityTime(long lastUserActivityTime) {

        if (DBWorkbench.getPlatform().getApplication() instanceof DBPApplicationDesktop app) {
            lastUserActivityTime = app.getLastUserActivityTime();
        }

        return lastUserActivityTime;
    }

    public void showNotification (DBPDataSource dataSource, DBPDataSourceContainer dsDescriptor, SMSession smSession) {
        DBeaverNotifications.showNotification(
                dataSource,
                DBeaverNotifications.NT_DISCONNECT_IDLE,
                "Connection '" + dsDescriptor.getName() + "' has been closed after long idle period",
                DBPMessageType.ERROR);
    }
}