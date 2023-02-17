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
package org.jkiss.dbeaver.model.websocket.event.datasource;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractProjectEvent;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;

import java.util.List;

public class WSDatasourceFolderEvent extends WSAbstractProjectEvent {
    private final List<String> nodePaths;

    private WSDatasourceFolderEvent(
        WSEventType eventType,
        String sessionId,
        String userId,
        String projectId,
        List<String> nodePaths
    ) {
        super(eventType, sessionId, userId, projectId);
        this.nodePaths = nodePaths;
    }

    public static WSDatasourceFolderEvent create(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> datasourceIds
    ) {
        return new WSDatasourceFolderEvent(
            WSEventType.DATASOURCE_FOLDER_CREATED,
            sessionId,
            userId,
            projectId,
            datasourceIds
        );
    }

    public static WSDatasourceFolderEvent delete(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> datasourceIds
    ) {
        return new WSDatasourceFolderEvent(
            WSEventType.DATASOURCE_FOLDER_DELETED,
            sessionId,
            userId,
            projectId,
            datasourceIds
        );
    }

    public static WSDatasourceFolderEvent update(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> datasourceIds
    ) {
        return new WSDatasourceFolderEvent(
            WSEventType.DATASOURCE_FOLDER_UPDATED,
            sessionId,
            userId,
            projectId,
            datasourceIds
        );
    }

    public List<String> getNodePaths() {
        return nodePaths;
    }
}
