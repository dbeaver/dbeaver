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
package org.jkiss.dbeaver.model.websocket.event.session;

import org.jkiss.dbeaver.model.websocket.event.WSEventType;
import org.jkiss.dbeaver.model.websocket.event.WSOutputLogInfo;

import java.util.List;

public class WSDBOutputLogEvent extends WSAbstractSessionEvent {
    private final String asyncTaskId;
    private final List<WSOutputLogInfo> messages;
    private final long eventTimestamp;

    public WSDBOutputLogEvent(String asyncTaskId,
                              List<WSOutputLogInfo> messages,
                              long eventTimestamp) {
        super(WSEventType.DB_LOG_UPDATED);
        this.asyncTaskId = asyncTaskId;
        this.messages = messages;
        this.eventTimestamp = eventTimestamp;
    }

    public String getAsyncTaskId() {
        return asyncTaskId;
    }

    public List<WSOutputLogInfo> getMessages() {
        return messages;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }
}
