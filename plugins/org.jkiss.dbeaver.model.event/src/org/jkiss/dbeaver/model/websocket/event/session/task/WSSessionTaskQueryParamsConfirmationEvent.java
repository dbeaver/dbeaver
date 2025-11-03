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
package org.jkiss.dbeaver.model.websocket.event.session.task;

import org.jkiss.code.NotNull;

import java.util.Map;

public class WSSessionTaskQueryParamsConfirmationEvent extends WSAbstractSessionTaskEvent {
    private static final String ID = "cb_session_task_query_params_confirmation_request";
    @NotNull
    private final String query;
    @NotNull
    private final Map<String, Object> parameters;

    public WSSessionTaskQueryParamsConfirmationEvent(
        @NotNull String taskId,
        @NotNull String title,
        @NotNull String message,
        @NotNull String query,
        @NotNull Map<String, Object> parameters
    ) {
        super(ID, taskId, title, message);
        this.query = query;
        this.parameters = parameters;
    }

    @NotNull
    public String getQuery() {
        return query;
    }

    @NotNull
    public Map<String, Object> getParameters() {
        return parameters;
    }
}
