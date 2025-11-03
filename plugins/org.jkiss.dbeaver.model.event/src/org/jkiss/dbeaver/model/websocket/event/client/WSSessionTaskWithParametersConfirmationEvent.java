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

import java.util.Map;

public class WSSessionTaskWithParametersConfirmationEvent extends WSClientEvent {
    public static final String ID = "cb_client_session_task_with_parameters_confirmation";
    @NotNull
    private final String taskId;
    @NotNull
    private final Map<String, Object> parameters;

    protected WSSessionTaskWithParametersConfirmationEvent(@NotNull String taskId, @NotNull Map<String, Object> parameters) {
        super(ID, WSConstants.TOPIC_SESSION_TASK);
        this.taskId = taskId;
        this.parameters = parameters;
    }

    @NotNull
    public String getTaskId() {
        return taskId;
    }

    @NotNull
    public Map<String, Object> getParameters() {
        return parameters;
    }
}
