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

package org.jkiss.dbeaver.runtime;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplicationWorkbench;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.impl.app.AbstractApplication;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Workbench
 */
public class DBWorkbench {

    private static final Log log = Log.getLog(DBWorkbench.class);

    private static DBPApplicationWorkbench applicationWorkbench;

    private static DBPApplicationWorkbench getApplicationWorkbench() {
        if (applicationWorkbench == null) {
            try {
                AbstractApplication.getInstance();
            } catch (Exception e) {
                log.debug("Error checking application instance", e);
            }
            applicationWorkbench = RuntimeUtils.getBundleService(DBPApplicationWorkbench.class, true);
        }
        return applicationWorkbench;
    }

    public static DBPPlatform getPlatform() {
        return getApplicationWorkbench().getPlatform();
    }

    public static <T extends DBPPlatform> T getPlatform(Class<T> pc) {
        return pc.cast(getPlatform());
    }

    public static boolean isPlatformStarted() {
        if (applicationWorkbench != null) {
            return applicationWorkbench.getPlatform() != null;
        }
        return false;
    }

    public static DBPPlatformUI getPlatformUI() {
        return getApplicationWorkbench().getPlatformUI();
    }

    /**
     * Service management
     */
    @Nullable
    public static <T> T getService(@NotNull Class<T> serviceType) {
        T service = ServiceRegistry.getInstance().getService(serviceType);
        if (service == null) {
            log.debug("Service '" + serviceType.getName() + "' not found");
        }
        return service;
    }

    /**
     * Distributed platform.
     * All configurations and resources are stored on remote servers.
     */
    public static boolean isDistributed() {
        return getPlatform().getApplication().isDistributed();
    }

    public static boolean hasFeature(@NotNull String feature) {
        return getPlatform().getApplication().hasProductFeature(feature);
    }

}
