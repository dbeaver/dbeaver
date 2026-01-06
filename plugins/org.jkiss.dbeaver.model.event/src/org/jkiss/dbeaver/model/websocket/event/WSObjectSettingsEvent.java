/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.model.websocket.WSConstants;

import java.util.Set;

public class WSObjectSettingsEvent extends WSAbstractEvent {
    public static final String UPDATED = "cb_object_settings_updated";
    public static final String DELETED = "cb_object_settings_deleted";

    private final SMObjectType smObjectType;
    private final String projectId;
    private final String objectId;
    private final Set<String> settingIds;

    protected WSObjectSettingsEvent(
        @NotNull String eventId,
        @Nullable String sessionId,
        @NotNull String userId,
        @NotNull SMObjectType smObjectType,
        @Nullable String projectId,
        @NotNull String objectId,
        @NotNull Set<String> settingIds
    ) {
        super(eventId, WSConstants.TOPIC_OBJECT_SETTINGS, sessionId, userId);
        this.smObjectType = smObjectType;
        this.projectId = projectId;
        this.objectId = objectId;
        this.settingIds = settingIds;
    }

    public static WSObjectSettingsEvent update(
        @Nullable String sessionId,
        @NotNull String userId,
        @NotNull SMObjectType smObjectType,
        @Nullable String projectId,
        @NotNull String objectId,
        @NotNull Set<String> settingIds
    ) {
        return new WSObjectSettingsEvent(
            UPDATED,
            sessionId,
            userId,
            smObjectType,
            projectId,
            objectId,
            settingIds
        );
    }

    public static WSObjectSettingsEvent delete(
        @Nullable String sessionId,
        @NotNull String userId,
        @NotNull SMObjectType smObjectType,
        @Nullable String projectId,
        @NotNull String objectId,
        @NotNull Set<String> settingIds
    ) {
        return new WSObjectSettingsEvent(
            UPDATED,
            sessionId,
            userId,
            smObjectType,
            projectId,
            objectId,
            settingIds
        );
    }

    @NotNull
    public SMObjectType getSmObjectType() {
        return smObjectType;
    }

    @Nullable
    public String getProjectId() {
        return projectId;
    }

    @NotNull
    public String getObjectId() {
        return objectId;
    }

    @NotNull
    public Set<String> getSettingIds() {
        return settingIds;
    }
}
