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
package org.jkiss.dbeaver.model.websocket.event.resource;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.rm.RMResource;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractProjectEvent;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;

public class WSResourceUpdatedEvent extends WSAbstractProjectEvent {
    @NotNull
    private final String resourcePath;
    @NotNull
    private final RMResource[] resourceParsedPath;

    private WSResourceUpdatedEvent(
        @NotNull WSEventType eventType,
        @Nullable String sessionId,
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull RMResource[] resourceParsedPath
    ) {
        super(eventType, sessionId, projectId);
        this.resourcePath = resourcePath;
        this.resourceParsedPath = resourceParsedPath;
    }

    public static WSResourceUpdatedEvent create(
        @Nullable String sessionId,
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull RMResource[] resourceParsedPath
    ) {
        return new WSResourceUpdatedEvent(
            WSEventType.RM_RESOURCE_CREATED,
            sessionId,
            projectId,
            resourcePath,
            resourceParsedPath
        );
    }

    public static WSResourceUpdatedEvent update(
        @Nullable String sessionId,
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull RMResource[] resourceParsedPath
    ) {
        return new WSResourceUpdatedEvent(
            WSEventType.RM_RESOURCE_UPDATED,
            sessionId,
            projectId,
            resourcePath,
            resourceParsedPath
        );
    }

    public static WSResourceUpdatedEvent delete(
        @Nullable String sessionId,
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull RMResource[] resourceParsedPath
    ) {
        return new WSResourceUpdatedEvent(
            WSEventType.RM_RESOURCE_DELETED,
            sessionId,
            projectId,
            resourcePath,
            resourceParsedPath
        );
    }

    @NotNull
    public String getProjectId() {
        return projectId;
    }

    @NotNull
    public String getResourcePath() {
        return resourcePath;
    }

    @NotNull
    public Object getResourceParsedPath() {
        return resourceParsedPath;
    }
}
