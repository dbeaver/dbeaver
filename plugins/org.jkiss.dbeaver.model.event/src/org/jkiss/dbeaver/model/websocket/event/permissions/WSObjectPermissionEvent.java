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
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.WSAbstractEvent;

public class WSObjectPermissionEvent extends WSAbstractEvent {
    public static final String UPDATED = "cb_object_permissions_updated";
    public static final String DELETED = "cb_object_permissions_deleted";

    private final SMObjectType smObjectType;
    private final String objectId;

    protected WSObjectPermissionEvent(
        @NotNull String eventId,
        @NotNull SMObjectType smObjectType,
        @NotNull String objectId,
        @Nullable String sessionId,
        @Nullable String userId
    ) {
        super(eventId, WSConstants.TOPIC_OBJECT_PERMISSIONS, sessionId, userId);
        this.smObjectType = smObjectType;
        this.objectId = objectId;
    }

    public static WSObjectPermissionEvent update(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull SMObjectType objectType,
        @NotNull String objectId
    ) {
        return new WSObjectPermissionEvent(
            UPDATED,
            objectType,
            objectId,
            sessionId,
            userId
        );
    }

    public static WSObjectPermissionEvent delete(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull SMObjectType objectType,
        @NotNull String objectId
    ) {
        return new WSObjectPermissionEvent(
            DELETED,
            objectType,
            objectId,
            sessionId,
            userId
        );
    }

    public SMObjectType getSmObjectType() {
        return smObjectType;
    }

    public String getObjectId() {
        return objectId;
    }
}
