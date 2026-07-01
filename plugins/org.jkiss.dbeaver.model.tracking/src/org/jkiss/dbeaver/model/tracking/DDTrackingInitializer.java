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
package org.jkiss.dbeaver.model.tracking;

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
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.atomic.AtomicBoolean;

public class DDTrackingInitializer implements IWorkbenchWindowInitializer {

    private static final Log log = Log.getLog(DDTrackingInitializer.class);

    private static final String ENV_KEY = "DATADAM_KEY";
    private static final String ENV_URL = "DATADAM_URL";
    private static final String DEFAULT_URL = "http://localhost:8080";

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile String trackingId;

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        String key = System.getenv(ENV_KEY);
        if (CommonUtils.isEmpty(key)) {
            log.debug("DataDam tracking disabled (no " + ENV_KEY + ")");
            return;
        }
        String envUrl = System.getenv(ENV_URL);
        String url = CommonUtils.isEmpty(envUrl) ? DEFAULT_URL : envUrl;
        DDTrackingClient client = new DDTrackingClient(url, key);

        AbstractJob startJob = new AbstractJob("DataDam tracking start") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                DDClientInfo info = new DDClientInfo(
                    DBWorkbench.getPlatform().getDeploymentId(),
                    DBWorkbench.getPlatform().getWorkspace().getWorkspaceId(),
                    GeneralUtils.getProductName(),
                    GeneralUtils.getProductVersion().toString(),
                    System.getProperty("os.name"),
                    localMacAddress(),
                    localIpAddress()
                );
                trackingId = client.start(info);
                return Status.OK_STATUS;
            }
        };
        startJob.setSystem(true);
        startJob.schedule();

        PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(@NotNull IWorkbench workbench, boolean forced) {
                String id = trackingId;
                if (id != null) {
                    client.stop(id);
                }
                return true;
            }

            @Override
            public void postShutdown(@NotNull IWorkbench workbench) {
            }
        });
    }

    @Nullable
    private static String localIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static String localMacAddress() {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (ni == null) {
                return null;
            }
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append(String.format("%02X", mac[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
