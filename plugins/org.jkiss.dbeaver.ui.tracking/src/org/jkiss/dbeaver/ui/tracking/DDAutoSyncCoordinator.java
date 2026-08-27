/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.tracking;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.app.DBPProjectManager;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.tracking.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyStore;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncService;
import org.jkiss.dbeaver.model.tracking.sync.core.DDTransportException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.concurrent.atomic.AtomicReference;

public class DDAutoSyncCoordinator {

    private static final Log log = Log.getLog(DDAutoSyncCoordinator.class);

    public static final String PREF_AUTO_SYNC_ENABLED = "datadam.sync.auto";

    private static final long DEBOUNCE_DELAY_MS = 15_000L;
    private static final long INITIAL_RETRY_DELAY_MS = 10_000L;
    private static final long MAX_RETRY_DELAY_MS = 300_000L;

    private static final AtomicReference<Session> ACTIVE_SESSION = new AtomicReference<>();

    private DDAutoSyncCoordinator() {
    }

    public static boolean isEnabled() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_AUTO_SYNC_ENABLED);
    }

    public static void setEnabled(boolean enabled) {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_AUTO_SYNC_ENABLED, enabled);
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    public static void start() {
        if (!isEnabled()) {
            return;
        }
        Session session = new Session(DBWorkbench.getPlatform().getWorkspace());
        if (ACTIVE_SESSION.compareAndSet(null, session)) {
            session.activate();
        }
    }

    public static void stop() {
        Session session = ACTIVE_SESSION.getAndSet(null);
        if (session != null) {
            session.deactivate();
        }
    }

    private static final class Session implements DBPEventListener, DBPProjectListener {

        private final DBPWorkspace workspace;
        private final AbstractJob job;
        private volatile long retryDelayMs = INITIAL_RETRY_DELAY_MS;

        private Session(@NotNull DBPWorkspace workspace) {
            this.workspace = workspace;
            this.job = new AbstractJob("DataDam auto-sync") {
                @NotNull
                @Override
                protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                    tick(monitor);
                    return Status.OK_STATUS;
                }
            };
            job.setUser(false);
            job.setSystem(true);
        }

        private void activate() {
            for (DBPProject project : workspace.getProjects()) {
                project.getDataSourceRegistry().addDataSourceListener(this);
            }
            if (workspace instanceof DBPProjectManager projectManager) {
                projectManager.addProjectListener(this);
            }
            scheduleTick(0);
        }

        private void deactivate() {
            job.cancel();
            for (DBPProject project : workspace.getProjects()) {
                project.getDataSourceRegistry().removeDataSourceListener(this);
            }
            if (workspace instanceof DBPProjectManager projectManager) {
                projectManager.removeProjectListener(this);
            }
        }

        @Override
        public void handleDataSourceEvent(@NotNull DBPEvent event) {
            if (!(event.getObject() instanceof DBPDataSourceContainer)) {
                return;
            }
            DBPEvent.Action action = event.getAction();
            if (action == DBPEvent.Action.OBJECT_ADD
                || action == DBPEvent.Action.OBJECT_REMOVE
                || (action == DBPEvent.Action.OBJECT_UPDATE && event.getEnabled() == null)
            ) {
                scheduleTick(DEBOUNCE_DELAY_MS);
            }
        }

        @Override
        public void handleProjectAdd(@NotNull DBPProject project) {
            project.getDataSourceRegistry().addDataSourceListener(this);
        }

        @Override
        public void handleProjectRemove(@NotNull DBPProject project) {
            project.getDataSourceRegistry().removeDataSourceListener(this);
        }

        private void scheduleTick(long delayMs) {
            job.cancel();
            job.schedule(delayMs);
        }

        private void tick(@NotNull DBRProgressMonitor monitor) {
            if (monitor.isCanceled() || ACTIVE_SESSION.get() != this || !isEnabled()) {
                return;
            }
            DDSyncService service = createSyncService();
            if (service == null || service.getBinding() == null) {
                return;
            }
            boolean transportFailed = false;
            try {
                service.upload();
            } catch (DDTransportException e) {
                transportFailed = true;
                log.debug("DataDam auto-sync upload failed", e);
            } catch (DBException e) {
                log.debug("DataDam auto-sync upload needs manual resolution, not retrying automatically", e);
            }
            if (monitor.isCanceled()) {
                return;
            }
            try {
                service.download();
            } catch (DDTransportException e) {
                transportFailed = true;
                log.debug("DataDam auto-sync download failed", e);
            } catch (DBException e) {
                log.debug("DataDam auto-sync download needs manual resolution, not retrying automatically", e);
            }
            if (transportFailed && ACTIVE_SESSION.get() == this) {
                log.debug("DataDam auto-sync retrying in " + retryDelayMs + " ms");
                scheduleTick(retryDelayMs);
                retryDelayMs = Math.min(retryDelayMs * 2, MAX_RETRY_DELAY_MS);
            } else if (!transportFailed) {
                retryDelayMs = INITIAL_RETRY_DELAY_MS;
            }
        }
    }

    @Nullable
    private static DDSyncService createSyncService() {
        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null) {
            return null;
        }
        String url = DDSyncPreferencePage.getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            return null;
        }
        return new DDSyncService(
            url,
            new DDBundleCredentials(bundle),
            DBWorkbench.getPlatform().getWorkspace(),
            bundle.accountId());
    }
}
