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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.WSConstants;

public class WSUserSecretEvent extends WSAbstractEvent {

    @NotNull
    private final String projectId;
    @NotNull
    private final String dataSourceId;

    public WSUserSecretEvent(
        @NotNull String projectId,
        @NotNull String dataSourceId,
        @Nullable String sessionId,
        @Nullable String userId
    ) {
        super("cb_user_secret_updated", WSConstants.TOPIC_USER_SECRET, sessionId, userId);
        this.projectId = projectId;
        this.dataSourceId = dataSourceId;
    }

    @NotNull
    public String getProjectId() {
        return projectId;
    }

    @NotNull
    public String getDataSourceId() {
        return dataSourceId;
    }
}
