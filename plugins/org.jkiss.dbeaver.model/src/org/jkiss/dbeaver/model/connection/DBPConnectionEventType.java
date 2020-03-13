/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.dbeaver.model.messages.ModelMessages;

/**
* Connection event type
*/
public enum DBPConnectionEventType {
    BEFORE_CONNECT(ModelMessages.model_connection_events_event_before_connect),
    AFTER_CONNECT(ModelMessages.model_connection_events_event_after_connect),
    BEFORE_DISCONNECT(ModelMessages.model_connection_events_event_before_disconnect),
    AFTER_DISCONNECT(ModelMessages.model_connection_events_event_after_disconnect);

    private final String title;

    DBPConnectionEventType(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}
