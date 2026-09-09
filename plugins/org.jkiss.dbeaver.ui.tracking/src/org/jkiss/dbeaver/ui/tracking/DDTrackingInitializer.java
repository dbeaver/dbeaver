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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.tracking.DDClientInfo;
import org.jkiss.dbeaver.model.tracking.DDTracking;
import org.jkiss.dbeaver.model.tracking.DDTrackingClient;
import org.jkiss.dbeaver.model.tracking.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyStore;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncCredentials;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DDTrackingInitializer implements IWorkbenchWindowInitializer {

    private static final Log log = Log.getLog(DDTrackingInitializer.class);

    private static final AtomicBoolean LISTENER_REGISTERED = new AtomicBoolean(false);
    private static final AtomicReference<Session> ACTIVE_SESSION = new AtomicReference<>();

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (LISTENER_REGISTERED.compareAndSet(false, true)) {
            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
                @Override
                public boolean preShutdown(@NotNull IWorkbench workbench, boolean forced) {
                    stop();
                    return true;
                }

                @Override
                public void postShutdown(@NotNull IWorkbench workbench) {
                    //empty
                }
            });
        }
        start();
    }

    /**
     * Starts tracking if a key bundle is available and tracking is not already running.
     * Safe to call again after a login, it is a no-op while a session is already active.
     */
    public static void start() {
        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null) {
            log.debug("DataDam tracking disabled (not logged in)");
            return;
        }
        String url = DDSyncPreferencePage.getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            log.debug("DataDam tracking disabled (no server URL)");
            return;
        }
        DDSyncCredentials credentials = new DDBundleCredentials(bundle);
        DDTrackingClient client = new DDTrackingClient(url);
        Session session = new Session(client, credentials);
        if (!ACTIVE_SESSION.compareAndSet(null, session)) {
            // already tracking
            return;
        }

        AbstractJob startJob = new AbstractJob("DataDam tracking start") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (ACTIVE_SESSION.get() != session) {
                    // stopped before the request went out
                    return Status.OK_STATUS;
                }
                DDClientInfo info = new DDClientInfo(
                    DBWorkbench.getPlatform().getDeploymentId(),
                    DBWorkbench.getPlatform().getWorkspace().getWorkspaceId(),
                    GeneralUtils.getProductName(),
                    GeneralUtils.getProductVersion().toString(),
                    System.getProperty(StandardConstants.ENV_OS_NAME),
                    RuntimeUtils.getOSVersion().toString(),
                    localMacAddress(),
                    localIpAddress()
                );
                DDTracking tracking = client.start(credentials, info);
                if (tracking != null) {
                    if (ACTIVE_SESSION.get() == session) {
                        session.trackingId.set(tracking.trackingId());
                    } else {
                        // stop() already ran and saw no trackingId yet - clean up the session we just started
                        client.stop(credentials, tracking.trackingId());
                    }
                }
                return Status.OK_STATUS;
            }
        };
        startJob.setSystem(true);
        startJob.schedule();
    }

    /**
     * Stops the active tracking session, if any. Safe to call after a logout or on shutdown.
     */
    public static void stop() {
        Session session = ACTIVE_SESSION.getAndSet(null);
        if (session == null) {
            return;
        }
        String id = session.trackingId.get();
        if (id != null) {
            session.client.stop(session.credentials, id);
        }
    }

    private static final class Session {
        @NotNull
        private final DDTrackingClient client;
        @NotNull
        private final DDSyncCredentials credentials;
        @NotNull
        private final AtomicReference<String> trackingId = new AtomicReference<>();

        private Session(@NotNull DDTrackingClient client, @NotNull DDSyncCredentials credentials) {
            this.client = client;
            this.credentials = credentials;
        }
    }

    @NotNull
    private static String localIpAddress() {
        return RuntimeUtils.getLocalHostOrLoopback().getHostAddress();
    }

    @Nullable
    private static String localMacAddress() {
        try {

            return HexFormat.ofDelimiter("-")
                .withUpperCase()
                .formatHex(RuntimeUtils.getLocalMacAddress());

        } catch (IOException e) {
            return null;
        }
    }

}
