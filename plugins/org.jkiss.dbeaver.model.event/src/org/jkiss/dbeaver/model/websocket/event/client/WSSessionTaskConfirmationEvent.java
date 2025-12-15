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
package org.jkiss.dbeaver.model.websocket.event.client;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.WSClientEvent;

public class WSSessionTaskConfirmationEvent extends WSClientEvent {
    public static final String ID = "cb_client_session_task_confirmation";

    private final String taskId;
    private final boolean confirmed;
    private final boolean skipConfirmations;

    public WSSessionTaskConfirmationEvent(
        @NotNull String taskId,
        boolean confirmed,
        boolean skipConfirmations
    ) {
        super(ID, WSConstants.TOPIC_SESSION_TASK);
        this.taskId = taskId;
        this.confirmed = confirmed;
        this.skipConfirmations = skipConfirmations;
    }

    public String getTaskId() {
        return taskId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isSkipConfirmations() {
        return skipConfirmations;
    }
}

