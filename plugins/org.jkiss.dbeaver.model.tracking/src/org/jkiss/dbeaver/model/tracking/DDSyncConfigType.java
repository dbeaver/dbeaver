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
package org.jkiss.dbeaver.model.tracking;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public enum DDSyncConfigType {

    AI("ai", ".metadata/.config/ai-configuration.json", DDSyncScope.WORKSPACE),
    CONNECTIONS("connections", ".dbeaver/data-sources.json", DDSyncScope.PROJECT);

    private final String id;
    private final String path;
    private final DDSyncScope scope;

    DDSyncConfigType(@NotNull String id, @NotNull String path, @NotNull DDSyncScope scope) {
        this.id = id;
        this.path = path;
        this.scope = scope;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public DDSyncScope getScope() {
        return scope;
    }

    @Nullable
    public static DDSyncConfigType findById(@NotNull String id) {
        for (DDSyncConfigType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
