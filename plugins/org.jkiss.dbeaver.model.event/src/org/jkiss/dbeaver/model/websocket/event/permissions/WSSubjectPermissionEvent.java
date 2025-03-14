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
package org.jkiss.dbeaver.model.websocket.event.permissions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.security.SMSubjectType;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractEvent;

public class WSSubjectPermissionEvent extends WSAbstractEvent {

    private final SMSubjectType subjectType;
    private final String subjectId;

    protected WSSubjectPermissionEvent(
        @NotNull SMSubjectType subjectType,
        @NotNull String subjectId,
        @Nullable String sessionId,
        @Nullable String userId
    ) {
        super("cb_subject_permissions_updated", WSConstants.TOPIC_SUBJECT_PERMISSIONS, sessionId, userId);
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }

    public static WSSubjectPermissionEvent update(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull SMSubjectType subjectType,
        @NotNull String subjectId
    ) {
        return new WSSubjectPermissionEvent(
            subjectType,
            subjectId,
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
}
