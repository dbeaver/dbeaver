/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

public class WSTransactionalCountEvent extends WSProjectResourceEvent {

    private final String contextId;
    private final String connectionId;
    private final int transactionalCount;

    public WSTransactionalCountEvent(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId,
        @NotNull String contextId,
        @NotNull String connectionId,
        int transactionalCount
    ) {
        super("cb_transaction_count", WSConstants.TOPIC_TRANSACTION_COUNT, sessionId, userId, projectId);
        this.connectionId = connectionId;
        this.transactionalCount = transactionalCount;
        this.contextId = contextId;
    }
}
