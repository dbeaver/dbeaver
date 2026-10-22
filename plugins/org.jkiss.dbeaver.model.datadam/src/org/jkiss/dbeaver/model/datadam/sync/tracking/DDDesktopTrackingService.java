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
package org.jkiss.dbeaver.model.datadam.sync.tracking;

import com.dbeaver.datadam.share.api.tracking.DDClientInfo;
import com.dbeaver.datadam.share.api.tracking.DDTracking;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.util.HexFormat;

/** Tracks the current desktop session across sign-ins. */
public class DDDesktopTrackingService {

    private static final DDDesktopTrackingService INSTANCE = new DDDesktopTrackingService();

    private final DDTrackingClient client;
    @Nullable
    private volatile DDTracking tracking;
    private volatile boolean isStopping;

    private DDDesktopTrackingService() {
        this.client = new DDTrackingClient(DDEndpoints.getStorageBaseUrl());
    }

    @NotNull
    public static DDDesktopTrackingService getInstance() {
        return INSTANCE;
    }

    public void start() throws DBException {
        if (tracking != null) {
            return;
        }
        isStopping = false;
        DDTracking started = client.start(clientInfo());
        if (!isStopping) {
            tracking = started;
        }
    }

    public void stop() throws DBException {
        isStopping = true;
        DDTracking current = tracking;
        if (current != null) {
            client.stop(current.trackingId());
            tracking = null;
        }
    }

    public void reset() {
        tracking = null;
    }

    @NotNull
    private DDClientInfo clientInfo() {
        return new DDClientInfo(
            DBWorkbench.getPlatform().getDeploymentId(),
            DBWorkbench.getPlatform().getWorkspace().getWorkspaceId(),
            GeneralUtils.getProductName(),
            GeneralUtils.getProductVersion().toString(),
            System.getProperty(StandardConstants.ENV_OS_NAME),
            RuntimeUtils.getOSVersion().toString(),
            localMacAddress(),
            localIpAddress()
        );
    }

    @NotNull
    private String localIpAddress() {
        return RuntimeUtils.getLocalHostOrLoopback().getHostAddress();
    }

    @Nullable
    private String localMacAddress() {
        try {
            return HexFormat.ofDelimiter("-")
                .withUpperCase()
                .formatHex(RuntimeUtils.getLocalMacAddress());
        } catch (IOException e) {
            return null;
        }
    }
}
