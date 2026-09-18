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
package org.jkiss.dbeaver.registry.nativeclient;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NativeClientRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.nativeClient";

    public static String TAG_NATIVE_CLIENTS = "nativeClients";
    public static String TAG_CLIENT = "client";
    private static NativeClientRegistry instance = null;

    private static final Map<String, NativeClientDescriptor> NATIVE_CLIENTS = new HashMap<>();

    private NativeClientRegistry(@NotNull IExtensionRegistry registry) {
        registry.getExtension(EXTENSION_ID).getConfigurationElements();
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement extConfig : extConfigs) {
            for (IConfigurationElement nativeClients : extConfig.getChildren(TAG_NATIVE_CLIENTS)) {
                for (IConfigurationElement child : nativeClients.getChildren(TAG_CLIENT)) {
                    NativeClientDescriptor clientDescriptor = new NativeClientDescriptor(child);
                    NATIVE_CLIENTS.put(clientDescriptor.getId(), clientDescriptor);
                }
            }
        }
    }

    public synchronized static NativeClientRegistry getInstance() {
        if (instance == null) {
            instance = new NativeClientRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @Nullable
    public NativeClientDescriptor getClient(@NotNull String id) {
        return NATIVE_CLIENTS.get(id);
    }
}
