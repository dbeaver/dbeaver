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
package org.jkiss.dbeaver.model.websocket.event.datasource;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractEvent;

public class WSNetworkProfileEvent extends WSAbstractEvent {
    public static final String UPDATED = "cb_network_profile_updated";
    @Nullable
    private final String projectId;

    private WSNetworkProfileEvent(
        @NotNull String eventId,
        @Nullable String sessionId,
        @Nullable String userId,
        @Nullable String projectId
    ) {
        super(eventId, WSConstants.TOPIC_PROFILES, sessionId, userId);
        this.projectId = projectId;
    }

    @NotNull
    public static WSNetworkProfileEvent update(
        @Nullable String sessionId,
        @Nullable String userId,
        @Nullable String projectId
    ) {
        return new WSNetworkProfileEvent(
            UPDATED,
            sessionId,
            userId,
            projectId
        );
    }

    @Nullable
    public String getProjectId() {
        return projectId;
    }
}
