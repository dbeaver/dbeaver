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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.qm.QMTransactionState;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;

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

        final DBPWorkspace workspace = platform.getWorkspace();
        for (DBPProject project : workspace.getProjects()) {
            if (project.isOpen() && project.isRegistryLoaded()) {
                DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();
                for (DBPDataSourceContainer ds : dataSourceRegistry.getDataSources()) {
                    checkDataSourceAlive(ds);
                }
            }
        }
        if (!platform.isShuttingDown()) {
            scheduleMonitor();
        }
        return Status.OK_STATUS;
    }

    private void checkDataSourceAlive(final DBPDataSourceContainer dataSourceDescriptor) {
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

        // End long transactions
        if (dataSourceDescriptor.isAutoCloseTransactions()) {
            endIdleTransactions(dataSourceDescriptor);
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

    private void endIdleTransactions(DBPDataSourceContainer dsDescriptor) {
        if (EndIdleTransactionsJob.isInProcess(dsDescriptor)) {
            return;
        }
        int ttlSeconds = dsDescriptor.getPreferenceStore().getInt(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL);
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        long lastUserActivityTime = application.getLastUserActivityTime();
        if (lastUserActivityTime <= 0 || (System.currentTimeMillis() - lastUserActivityTime) / 1000 < ttlSeconds) {
            return;
        }
        DBPDataSource dataSource = dsDescriptor.getDataSource();
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
        }
    }

    public void scheduleMonitor() {
        schedule(MONITOR_INTERVAL);
    }

}