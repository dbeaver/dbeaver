/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.websocket.event.client;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.websocket.event.WSClientEvent;
import org.jkiss.dbeaver.model.websocket.event.WSClientEventType;
import org.jkiss.dbeaver.model.websocket.event.WSEventTopic;

import java.util.Set;

/**
 * Subscribe on event topic
 */
public class WSUpdateActiveProjectsClientEvent extends WSClientEvent {
    @NotNull
    private final Set<String> projectIds;

    public WSUpdateActiveProjectsClientEvent(@NotNull Set<String> projectIds) {
        super(WSClientEventType.ACTIVE_PROJECTS.getEventId(), WSEventTopic.PROJECTS.getTopicId());
        this.projectIds = projectIds;
    }

    @NotNull
    public Set<String> getProjectIds() {
        return projectIds;
    }
}
