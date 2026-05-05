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
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.websocket.WSEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WSEventHandlersRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.ws.event.handler";
    private static final String EVENT_HANDLER_TAG = "eventHandler";
    private static WSEventHandlersRegistry instance = null;

    public synchronized static WSEventHandlersRegistry getInstance() {
        if (instance == null) {
            instance = new WSEventHandlersRegistry();
        }
        return instance;
    }

    @NotNull
    public List<WSEventHandler> getEventHandlers() {
        List<WSEventHandlerDescriptor> eventHandlerDescriptors = readDescriptors();

        return eventHandlerDescriptors.stream()
            .map(WSEventHandlerDescriptor::getInstance)
            .collect(Collectors.toList());
    }

    @NotNull
    public List<WSEventHandlerDescriptor> readDescriptors() {
        var result = new ArrayList<WSEventHandlerDescriptor>();
        var registry = Platform.getExtensionRegistry();
        for (IConfigurationElement ext : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            // Load webServices
            if (EVENT_HANDLER_TAG.equals(ext.getName())) {
                result.add(new WSEventHandlerDescriptor(ext));
            }
        }
        return result;
    }
}
