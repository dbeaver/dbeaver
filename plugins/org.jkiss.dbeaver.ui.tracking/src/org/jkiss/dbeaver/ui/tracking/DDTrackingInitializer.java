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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.tracking.DDClientInfo;
import org.jkiss.dbeaver.model.tracking.DDTrackStop;
import org.jkiss.dbeaver.model.tracking.DDTracking;
import org.jkiss.dbeaver.model.tracking.DDTrackingClient;
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

    private static final String ENV_KEY = "DATADAM_KEY";
    private static final String ENV_URL = "DATADAM_URL";

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        String key = readAccessKey();
        if (CommonUtils.isEmpty(key)) {
            log.debug("DataDam tracking disabled (no access key)");
            return;
        }
        String url = System.getenv(ENV_URL);
        if (CommonUtils.isEmpty(url)) {
            log.debug("DataDam tracking disabled (no " + ENV_URL + ")");
            return;
        }
        DDTrackingClient client = new DDTrackingClient(url);
        AtomicReference<String> trackingId = new AtomicReference<>();

        AbstractJob startJob = new AbstractJob("DataDam tracking start") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
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
                DDTracking tracking = client.start(key, info);
                if (tracking != null) {
                    trackingId.set(tracking.trackingId());
                }
                return Status.OK_STATUS;
            }
        };
        startJob.setSystem(true);
        startJob.schedule();

        PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(@NotNull IWorkbench workbench, boolean forced) {
                String id = trackingId.get();
                if (id != null) {
                    client.stop(key, new DDTrackStop(id));
                }
                return true;
            }

            @Override
            public void postShutdown(@NotNull IWorkbench workbench) {
                //empty
            }
        });
    }

    @Nullable
    private static String readAccessKey() {
        try {
            String key = DBSSecretController.getGlobalSecretController()
                .getPrivateSecretValue(DDSyncPreferencePage.SECRET_ACCESS_KEY);
            if (!CommonUtils.isEmpty(key)) {
                return key;
            }
        } catch (DBException e) {
            log.debug("Error reading access key from secure storage", e);
        }
        return System.getenv(ENV_KEY);
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
