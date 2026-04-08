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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class DBPAuthPromptField {
    private final String id;
    private final String label;
    private final String description;
    private final boolean password;
    private final String value;

    public DBPAuthPromptField(
        @NotNull String id,
        @NotNull String label,
        @Nullable String description,
        boolean password,
        @Nullable String value
    ) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.password = password;
        this.value = value;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public boolean isPassword() {
        return password;
    }

    @Nullable
    public String getValue() {
        return value;
    }
}
