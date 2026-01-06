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
package org.jkiss.dbeaver.model.websocket.event.datasource;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.WSProjectResourceEvent;

import java.util.List;

public class WSDataSourceEvent extends WSProjectResourceEvent {
    public static final String CREATED = "cb_datasource_created";
    public static final String DELETED = "cb_datasource_deleted";
    public static final String UPDATED = "cb_datasource_updated";
    @NotNull
    private final List<String> dataSourceIds;
    @NotNull
    private final WSDataSourceProperty property;

    private WSDataSourceEvent(
        @NotNull String eventId,
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> dataSourceIds,
        @NotNull WSDataSourceProperty property
    ) {
        super(eventId, WSConstants.TOPIC_DATASOURCE, sessionId, userId, projectId);
        this.dataSourceIds = dataSourceIds;
        this.property = property;
    }

    public static WSDataSourceEvent create(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> datasourceIds,
        @NotNull WSDataSourceProperty property
    ) {
        return new WSDataSourceEvent(
            CREATED,
            sessionId,
            userId,
            projectId,
            datasourceIds,
            property);
    }

    public static WSDataSourceEvent delete(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> datasourceIds,
        @NotNull WSDataSourceProperty property
    ) {
        return new WSDataSourceEvent(
            DELETED,
            sessionId,
            userId,
            projectId,
            datasourceIds,
            property);
    }

    public static WSDataSourceEvent update(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull List<String> datasourceIds,
        @NotNull WSDataSourceProperty property
    ) {
        return new WSDataSourceEvent(
            UPDATED,
            sessionId,
            userId,
            projectId,
            datasourceIds,
            property);
    }

    @NotNull
    public List<String> getDataSourceIds() {
        return dataSourceIds;
    }

    @NotNull
    public WSDataSourceProperty getProperty() {
        return property;
    }
}
