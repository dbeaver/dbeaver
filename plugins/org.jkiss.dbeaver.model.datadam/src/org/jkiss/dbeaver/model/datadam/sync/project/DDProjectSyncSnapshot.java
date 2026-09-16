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
package org.jkiss.dbeaver.model.datadam.sync.project;

import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.utils.Pair;

import java.util.Map;
import java.util.UUID;

/**
 * Consistent local and server state used by a project synchronization operation.
 */
public record DDProjectSyncSnapshot(
    @NotNull UUID remoteProjectId,
    @NotNull DDSharedProjectRevision serverRevision,
    @NotNull Map<String, Pair<String, byte[]>> files,
    @NotNull String configurationFingerprint,
    @NotNull DDSyncChange change
) {
}
