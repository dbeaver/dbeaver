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
package org.jkiss.dbeaver.model.websocket.event.datasource;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractProjectEvent;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;

import java.util.List;

public class WSDataSourceEvent extends WSAbstractProjectEvent {
    @Nullable
    private final List<String> dataSourceIds;

    private WSDataSourceEvent(
        @NotNull WSEventType eventType,
        @Nullable String sessionId,
        @NotNull String projectId,
        @Nullable List<String> dataSourceIds
    ) {
        super(eventType, sessionId, projectId);
        this.dataSourceIds = dataSourceIds;
    }

    public static WSDataSourceEvent create(
        @Nullable String sessionId,
        @NotNull String projectId,
        @Nullable List<String> datasourceIds
    ) {
        return new WSDataSourceEvent(
            WSEventType.DATASOURCE_CREATED,
            sessionId,
            projectId,
            datasourceIds
        );
    }

    public static WSDataSourceEvent delete(
        @Nullable String sessionId,
        @NotNull String projectId,
        @Nullable List<String> datasourceIds
    ) {
        return new WSDataSourceEvent(
            WSEventType.DATASOURCE_DELETED,
            sessionId,
            projectId,
            datasourceIds
        );
    }

    public static WSDataSourceEvent update(
        @Nullable String sessionId,
        @NotNull String projectId,
        @Nullable List<String> datasourceIds
    ) {
        return new WSDataSourceEvent(
            WSEventType.DATASOURCE_UPDATED,
            sessionId,
            projectId,
            datasourceIds
        );
    }

    @Nullable
    public List<String> getDataSourceIds() {
        return dataSourceIds;
    }
}
