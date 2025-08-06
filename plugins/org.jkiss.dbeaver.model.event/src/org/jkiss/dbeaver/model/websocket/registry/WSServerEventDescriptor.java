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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.websocket.event.WSEvent;

/**
 *  Event from the server to the client (browser or desktop)
 */
public class WSServerEventDescriptor extends WSAbstractEventDescriptor {
    @NotNull
    private final String topicId;

    protected WSServerEventDescriptor(
        @NotNull IConfigurationElement cfg
    ) {
        super(cfg);
        this.topicId = cfg.getAttribute("topicId");;
    }
    @NotNull
    public String getTopicId() {
        return topicId;
    }

    @NotNull
    public Class<? extends WSEvent> getEventClass() {
        return implType.getObjectClass(WSEvent.class);
    }
}
