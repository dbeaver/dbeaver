/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.Adapters;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

/**
 * Workbench
 */
public class DBWorkbench {

    private static final Log log = Log.getLog(DBWorkbench.class);

    private static final DBWorkbench instance = new DBWorkbench();

    private static volatile DBPPlatform platformInstance = null;
    private static volatile DBPPlatformUI platformUIInstance = null;

    public static DBPPlatform getPlatform() {
        if (platformInstance == null) {
            synchronized (DBWorkbench.class) {
                if (platformInstance == null) {
                    platformInstance = Adapters.adapt(instance, DBPPlatform.class);
                    if (platformInstance == null) {
                        throw new IllegalStateException("Internal configuration error. Platform not instantiated.");
                    }
                }
            }
        }
        return platformInstance;
    }

    public static DBPPlatformUI getPlatformUI() {
        if (platformUIInstance == null) {
            synchronized (DBWorkbench.class) {
                if (platformUIInstance == null) {
                    platformUIInstance = Adapters.adapt(instance, DBPPlatformUI.class);
                    if (platformUIInstance == null) {
                        throw new IllegalStateException("Internal configuration error. Platform UI not instantiated.");
                    }
                }
            }
        }
        return platformUIInstance;
    }

    /**
     * Service management
     */
    @Nullable
    public static <T> T getService(@NotNull Class<T> serviceType) {
        T service = ServiceRegistry.getInstance().getService(serviceType);
        if (service == null) {
            log.error("Service '" + serviceType.getName() + "' not found");
        }
        return service;
    }

}
