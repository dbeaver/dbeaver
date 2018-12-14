/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

/**
 * Workbench
 */
public class DBWorkbench {

    private static DBWorkbench instance = new DBWorkbench();

    public static DBPPlatform getPlatform() {
        DBPPlatform platform = Adapters.adapt(instance, DBPPlatform.class);
        if (platform == null) {
            throw new IllegalStateException("Internal configuration error. Platform not instantiated.");
        }
        return platform;
    }

    public static DBPPlatformUI getPlatformUI() {
        DBPPlatformUI platformUI = Adapters.adapt(instance, DBPPlatformUI.class);
        if (platformUI == null) {
            throw new IllegalStateException("Internal configuration error. Platform UI not instantiated.");
        }
        return platformUI;
    }

    /**
     * Service management
     */
    @Nullable
    public static <T> T getService(@NotNull Class<T> serviceType) {
        return ServiceRegistry.getInstance().getService(serviceType);
    }

}
