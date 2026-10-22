/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam.project;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;

import java.time.LocalDateTime;
import java.util.UUID;

/** A project in the current DataDam account, with or without a local workspace copy. */
public record DDProjectView(
    @NotNull UUID id,
    @NotNull String name,
    @Nullable String description,
    @Nullable DBPProject localProject,
    @Nullable DDSharedProject remoteProject,
    @Nullable LocalDateTime lastSyncedRevision,
    @NotNull Status status
) {
    public enum Status {
        NOT_ON_DEVICE,
        MISSING_ON_SERVER,
        UP_TO_DATE,
        LOCAL_CHANGES,
        SERVER_CHANGES,
        CONFLICT
    }
}
