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
package org.jkiss.dbeaver.model.websocket.event;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.gson.HiddenField;

/**
 * Base websocket event
 */
public abstract class WSAbstractEvent implements WSEvent {
    @Nullable
    @HiddenField
    private final String sessionId;
    @Nullable
    @HiddenField
    private final String userId;
    @NotNull
    private final String id;
    @NotNull
    private final String topicId;
    private final long timestamp = System.currentTimeMillis();

    protected WSAbstractEvent(@NotNull WSEventType eventType) {
        this(eventType, null, null);
    }

    protected WSAbstractEvent(@NotNull WSEventType eventType, @Nullable String sessionId, @Nullable String userId) {
        this.id = eventType.getEventId();
        this.topicId = eventType.getTopic().getTopicId();
        this.sessionId = sessionId;
        this.userId = userId;
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    @Override
    @NotNull
    public String getTopicId() {
        return topicId;
    }

    @Override
    @Nullable
    public String getUserId() {
        return userId;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}