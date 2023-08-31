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
package org.jkiss.dbeaver.model.websocket.event.permissions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.security.SMSubjectType;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractEvent;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;

public class WSSubjectPermissionEvent extends WSAbstractEvent {

    private final SMSubjectType subjectType;
    private final String subjectId;
    private final boolean subjectConnectionsChanged;

    protected WSSubjectPermissionEvent(
        @NotNull WSEventType eventType,
        @NotNull SMSubjectType subjectType,
        @NotNull String subjectId,
        boolean subjectConnectionsChanged,
        @Nullable String sessionId,
        @Nullable String userId
    ) {
        super(eventType, sessionId, userId);
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.subjectConnectionsChanged = subjectConnectionsChanged;
    }

    public static WSSubjectPermissionEvent update(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull SMSubjectType subjectType,
        @NotNull String subjectId,
        boolean subjectConnectionsChanged
    ) {
        return new WSSubjectPermissionEvent(
            WSEventType.SUBJECT_PERMISSIONS_UPDATED,
            subjectType,
            subjectId,
            subjectConnectionsChanged,
            sessionId,
            userId
        );
    }

    public String getSubjectId() {
        return subjectId;
    }

    public SMSubjectType getSubjectType() {
        return subjectType;
    }

    public boolean isSubjectConnectionsChanged() {
        return subjectConnectionsChanged;
    }
}
