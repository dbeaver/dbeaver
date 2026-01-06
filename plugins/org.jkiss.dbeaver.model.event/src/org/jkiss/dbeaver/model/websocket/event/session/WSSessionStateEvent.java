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
package org.jkiss.dbeaver.model.websocket.event.session;

import org.jkiss.dbeaver.model.websocket.WSConstants;

import java.util.Map;

public class WSSessionStateEvent extends WSAbstractSessionEvent {
    private final long lastAccessTime;
    private final long remainingTime;
    private final boolean isValid;
    private final boolean isCacheExpired;
    private final String locale;
    private final Map<String, Object> actionParameters;

    public WSSessionStateEvent(
        long lastAccessTime,
        long remainingTime,
        boolean isValid,
        boolean isCacheExpired,
        String locale,
        Map<String, Object> actionParameters
    ) {
        super("cb_session_state", WSConstants.TOPIC_SESSION);
        this.lastAccessTime = lastAccessTime;
        this.remainingTime = remainingTime;
        this.isValid = isValid;
        this.isCacheExpired = isCacheExpired;
        this.locale = locale;
        this.actionParameters = actionParameters;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean isCacheExpired() {
        return isCacheExpired;
    }

    public String getLocale() {
        return locale;
    }

    public Map<String, Object> getActionParameters() {
        return actionParameters;
    }
}
