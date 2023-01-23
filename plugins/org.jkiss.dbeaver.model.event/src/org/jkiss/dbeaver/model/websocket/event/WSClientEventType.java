/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.websocket.event;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.event.client.WSSubscribeOnTopicClientEvent;
import org.jkiss.dbeaver.model.websocket.event.client.WSUnsubscribeFromTopicClientEvent;
import org.jkiss.dbeaver.model.websocket.event.client.WSUpdateActiveProjectsClientEvent;

public enum WSClientEventType {
    TOPIC_SUBSCRIBE("cb_client_topic_subscribe", WSSubscribeOnTopicClientEvent.class),
    TOPIC_UNSUBSCRIBE("cb_client_topic_unsubscribe", WSUnsubscribeFromTopicClientEvent.class),
    ACTIVE_PROJECTS("cb_client_projects_active", WSUpdateActiveProjectsClientEvent.class),
    ;

    private final String eventId;
    private final Class<? extends WSClientEvent> eventClass;

    WSClientEventType(String eventId, Class<? extends WSClientEvent> eventClass) {
        this.eventId = eventId;
        this.eventClass = eventClass;
    }

    public String getEventId() {
        return eventId;
    }

    public Class<? extends WSClientEvent> getEventClass() {
        return eventClass;
    }


    @Nullable
    public static WSClientEventType valueById(String id) {
        for (WSClientEventType value : values()) {
            if (value.getEventId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
