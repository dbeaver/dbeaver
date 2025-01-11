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
package org.jkiss.dbeaver.model.websocket.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WSEventRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.ws.event";
    private static final String EVENT_TAG = "event";
    private static final String CLIENT_EVENT_TAG = "clientEvent";

    private static WSEventRegistry instance = null;
    private final List<WSServerEventDescriptor> serverEvents = new ArrayList<>();
    private final List<WSClientEventDescriptor> clientEvents = new ArrayList<>();

    private WSEventRegistry() {
    }

    public synchronized static WSEventRegistry getInstance() {
        if (instance == null) {
            instance = new WSEventRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private void loadExtensions(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (EVENT_TAG.equals(ext.getName())) {
                var descriptor = new WSServerEventDescriptor(ext);
                serverEvents.add(descriptor);
            } else if(CLIENT_EVENT_TAG.equals(ext.getName())) {
                var descriptor = new WSClientEventDescriptor(ext);
                clientEvents.add(descriptor);
            }
        }
    }

    @NotNull
    public List<WSServerEventDescriptor> getServerEvents() {
        return new ArrayList<>(serverEvents);
    }

    @NotNull
    public List<WSClientEventDescriptor> getClientEvents() {
        return new ArrayList<>(clientEvents);
    }
}
