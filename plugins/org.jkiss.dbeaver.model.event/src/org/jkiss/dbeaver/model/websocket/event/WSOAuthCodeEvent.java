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
package org.jkiss.dbeaver.model.websocket.event;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.WSConstants;

public class WSOAuthCodeEvent extends WSAbstractEvent {

    private final boolean isError;
    private final String state;
    private final String message;

    public WSOAuthCodeEvent(
        @Nullable String sessionId,
        @NotNull String state,
        @Nullable String message,
        boolean isError
    ) {
        super("cb_oauth_code", WSConstants.TOPIC_DB_OAUTH, sessionId, null);
        this.state = state;
        this.message = message;
        this.isError = isError;
    }

    public boolean isError() {
        return isError;
    }

    @NotNull
    public String getState() {
        return state;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}
