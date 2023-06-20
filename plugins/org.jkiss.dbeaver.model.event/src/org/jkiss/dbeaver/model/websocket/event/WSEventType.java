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
import org.jkiss.dbeaver.model.websocket.event.datasource.WSDataSourceEvent;
import org.jkiss.dbeaver.model.websocket.event.datasource.WSDatasourceFolderEvent;
import org.jkiss.dbeaver.model.websocket.event.permissions.WSObjectPermissionEvent;
import org.jkiss.dbeaver.model.websocket.event.resource.WSResourceUpdatedEvent;
import org.jkiss.dbeaver.model.websocket.event.session.WSSessionExpiredEvent;
import org.jkiss.dbeaver.model.websocket.event.session.WSSessionStateEvent;
import org.jkiss.dbeaver.model.websocket.event.session.WSSocketConnectedEvent;

public enum WSEventType {
    SERVER_CONFIG_CHANGED(
        "cb_config_changed",
        WSEventTopic.SERVER_CONFIG,
        WSServerConfigurationChangedEvent.class
    ),

    SESSION_LOG_UPDATED(
        "cb_session_log_updated",
        WSEventTopic.SESSION_LOG,
        WSSessionLogUpdatedEvent.class
    ),

    SESSION_WEBSOCKET_CONNECTED("cb_session_websocket_connected", WSEventTopic.SESSION, WSSocketConnectedEvent.class),
    SESSION_STATE("cb_session_state", WSEventTopic.SESSION, WSSessionStateEvent.class),
    SESSION_EXPIRED("cb_session_expired", WSEventTopic.SESSION, WSSessionExpiredEvent.class),

    DATASOURCE_CREATED("cb_datasource_created", WSEventTopic.DATASOURCE, WSDataSourceEvent.class),
    DATASOURCE_UPDATED("cb_datasource_updated", WSEventTopic.DATASOURCE, WSDataSourceEvent.class),
    DATASOURCE_DELETED("cb_datasource_deleted", WSEventTopic.DATASOURCE, WSDataSourceEvent.class),


    DATASOURCE_FOLDER_CREATED(
        "cb_datasource_folder_created",
        WSEventTopic.DATASOURCE_FOLDER,
        WSDatasourceFolderEvent.class
    ),
    DATASOURCE_FOLDER_UPDATED(
        "cb_datasource_folder_updated",
        WSEventTopic.DATASOURCE_FOLDER,
        WSDatasourceFolderEvent.class
    ),
    DATASOURCE_FOLDER_DELETED(
        "cb_datasource_folder_deleted",
        WSEventTopic.DATASOURCE_FOLDER,
        WSDatasourceFolderEvent.class
    ),

    OBJECT_PERMISSIONS_UPDATED("cb_object_permissions_updated", WSEventTopic.PERMISSIONS, WSObjectPermissionEvent.class),

    DATASOURCE_SECRET_UPDATED("cb_user_secret_updated", WSEventTopic.USER_SECRET, WSUserSecretEvent.class),

    RM_RESOURCE_CREATED("cb_rm_resource_created", WSEventTopic.RM_SCRIPTS, WSResourceUpdatedEvent.class),
    RM_RESOURCE_UPDATED("cb_rm_resource_updated", WSEventTopic.RM_SCRIPTS, WSResourceUpdatedEvent.class),
    RM_RESOURCE_DELETED("cb_rm_resource_deleted", WSEventTopic.RM_SCRIPTS, WSResourceUpdatedEvent.class),

    RM_PROJECT_ADDED("cb_rm_project_added", WSEventTopic.PROJECTS, WSProjectUpdateEvent.class),
    RM_PROJECT_REMOVED("cb_rm_project_removed", WSEventTopic.PROJECTS, WSProjectUpdateEvent.class),

    WORKSPACE_CONFIG_CHANGED("cb_workspace_config_changed", WSEventTopic.WORKSPACE_CONFIG, WSWorkspaceConfigurationChangedEvent.class);

    private final String eventId;
    private final WSEventTopic topic;
    private final Class<? extends WSEvent> eventClass;

    WSEventType(String eventId, WSEventTopic topic, Class<? extends WSEvent> eventClass) {
        this.eventId = eventId;
        this.topic = topic;
        this.eventClass = eventClass;
    }

    @Nullable
    public static WSEventType valueById(String id) {
        for (WSEventType value : values()) {
            if (value.getEventId().equals(id)) {
                return value;
            }
        }
        return null;
    }

    public String getEventId() {
        return eventId;
    }

    public WSEventTopic getTopic() {
        return topic;
    }

    public Class<? extends WSEvent> getEventClass() {
        return eventClass;
    }
}
