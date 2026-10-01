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
package org.jkiss.dbeaver.model.ai.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public record QMAIDataSource(
    @NotNull String projectId,
    @NotNull String dataSourceId
) {
    @NotNull
    public String asString() {
        return dataSourceId + "@" + projectId;
    }

    @Nullable
    public static QMAIDataSource fromString(@Nullable String srt) {
        if (srt == null) {
            return null;
        }

        String[] parts = srt.split("@");
        if (parts.length != 2) {
            return null;
        }
        return new QMAIDataSource(parts[1], parts[0]);
    }
}
