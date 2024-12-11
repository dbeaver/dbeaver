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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;

/**
 * Async task info event.
 */
public class WSSessionTaskInfoEvent extends WSAbstractSessionEvent {
    @NotNull
    private final String taskId;
    @Nullable
    private final String statusName;
    private final boolean running;

    public WSSessionTaskInfoEvent(
        @NotNull String taskId,
        @Nullable String statusName,
        boolean running
    ) {
        super(WSEventType.SESSION_TASK_INFO_UPDATED);
        this.taskId = taskId;
        this.statusName = statusName;
        this.running = running;
    }

    @NotNull
    public String getTaskId() {
        return taskId;
    }

    @Nullable
    public String getStatusName() {
        return statusName;
    }

    public boolean isRunning() {
        return running;
    }
}
