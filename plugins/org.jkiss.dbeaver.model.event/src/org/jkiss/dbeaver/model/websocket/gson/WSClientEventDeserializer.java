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
package org.jkiss.dbeaver.model.websocket.gson;

import org.jkiss.dbeaver.model.websocket.event.WSClientEvent;
import org.jkiss.dbeaver.model.websocket.registry.WSClientEventDescriptor;
import org.jkiss.dbeaver.model.websocket.registry.WSEventRegistry;

import java.util.stream.Collectors;

public class WSClientEventDeserializer extends WSAbstractClassByIdDeserializer<WSClientEvent> {
    public WSClientEventDeserializer() {
        super(
            WSEventRegistry.getInstance().getClientEvents()
                .stream()
                .collect(
                    Collectors.toMap(
                        WSClientEventDescriptor::getId,
                        WSClientEventDescriptor::getEventClass
                    )
                )
        );
    }
}
