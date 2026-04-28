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
package org.jkiss.dbeaver.model.websocket.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.websocket.event.WSClientEvent;
import org.jkiss.dbeaver.model.websocket.event.WSClientEventHandler;
import org.jkiss.utils.CommonUtils;

/**
 * Event from the client (browser or desktop) to the server
 */
public class WSClientEventDescriptor extends WSAbstractEventDescriptor {

    private final static Log log = Log.getLog(WSClientEventDescriptor.class);

    private WSClientEventHandler<WSClientEvent> handler;

    protected WSClientEventDescriptor(
        @NotNull IConfigurationElement cfg
    ) {
        super(cfg);
        if (!CommonUtils.isEmpty(cfg.getAttribute("handler"))) {
            ObjectType handlerType = new ObjectType(cfg, "handler");
            try {
                handler = handlerType.createInstance(WSClientEventHandler.class);
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    @NotNull
    public Class<? extends WSClientEvent> getEventClass() {
        return implType.getImplClass(WSClientEvent.class);
    }

    @Nullable
    public WSClientEventHandler<WSClientEvent> getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return "WSClientEvent " + getId();
    }
}
