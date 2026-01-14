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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.WSConstants;

public class WSServerConfigurationChangedEvent extends WSAbstractEvent {
    private static final String ID = "cb_config_changed";

    public WSServerConfigurationChangedEvent() {
        super(ID, WSConstants.TOPIC_SERVER_CONFIG);
    }

    public WSServerConfigurationChangedEvent(@Nullable String sessionId, @Nullable String userId) {
        super(ID, WSConstants.TOPIC_SERVER_CONFIG, sessionId, userId);
    }
}
