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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.*;

/**
 * KeepAliveListenerJob
 */
public class KeepAliveListenerJob extends AbstractJob
{
    private static final int MONITOR_INTERVAL = 3000; // once per 3 seconds
    private static final long SYSTEM_SUSPEND_INTERVAL = 30000; // 30 seconds of inactivity - most likely a system suspend

    private static final Log log = Log.getLog(KeepAliveListenerJob.class);

    private final DBPPlatform platform;
    private Map<String, Long> checkCache = new HashMap<>();
    private final Set<String> pingCache = new HashSet<>();
    private long lastPingTime = -1;

    public KeepAliveListenerJob(DBPPlatform platform)
    {
        super("Keep-Alive monitor");
        setUser(false);
        setSystem(true);
        this.platform = platform;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
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
        final int keepAliveInterval = dataSourceDescriptor.getConnectionConfiguration().getKeepAliveInterval();
        if (keepAliveInterval <= 0) {
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
            final KeepAlivePingJob pingJob = new KeepAlivePingJob(dataSource);
            pingJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    synchronized (KeepAliveListenerJob.this) {
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

    public void scheduleMonitor() {
        schedule(MONITOR_INTERVAL);
    }

}