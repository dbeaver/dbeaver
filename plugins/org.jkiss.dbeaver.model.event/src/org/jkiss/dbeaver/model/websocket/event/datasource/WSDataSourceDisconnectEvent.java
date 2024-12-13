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
import org.jkiss.dbeaver.model.websocket.event.WSAbstractEvent;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;
import org.jkiss.dbeaver.model.websocket.event.WSProjectResourceEvent;

public class WSDataSourceDisconnectEvent extends WSProjectResourceEvent {

    @NotNull
    private final String connectionId;

    public WSDataSourceDisconnectEvent(
        @NotNull String projectId,
        @NotNull String connectionId,
        @NotNull String sessionId,
        @NotNull String userId
    ) {
        super(WSEventType.DATASOURCE_DISCONNECTED, sessionId, userId, projectId);
        this.connectionId = connectionId;
    }

    @NotNull
    public String getConnectionId() {
        return connectionId;
    }
}
