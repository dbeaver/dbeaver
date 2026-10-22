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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class QMAIContext {
    private static final Gson GSON = new Gson();
    private static final String ADDITIONAL_DATA_SOURCES_PROPERTY = "additionalDataSources";

    private final String contextJson;
    private final Set<QMAIContextObject> objects;

    public QMAIContext(
        @Nullable String contextJson,
        @NotNull Set<QMAIContextObject> objects
    ) {
        this.contextJson = contextJson;
        this.objects = objects;
    }

    @Nullable
    public String getContextJson() {
        return contextJson;
    }

    @NotNull
    public QMAIContext withAdditionalDataSources(@NotNull List<QMAIDataSource> dataSources) {
        if (dataSources.isEmpty()) {
            return this;
        }
        JsonObject json = contextJson == null ? new JsonObject() : GSON.fromJson(contextJson, JsonObject.class);
        json.add(ADDITIONAL_DATA_SOURCES_PROPERTY, GSON.toJsonTree(dataSources));
        return new QMAIContext(GSON.toJson(json), objects);
    }

    @NotNull
    public List<QMAIDataSource> getAdditionalDataSources() {
        if (contextJson == null) {
            return List.of();
        }
        JsonObject json = GSON.fromJson(contextJson, JsonObject.class);
        if (!json.has(ADDITIONAL_DATA_SOURCES_PROPERTY)) {
            return List.of();
        }
        QMAIDataSource[] dataSources = GSON.fromJson(json.get(ADDITIONAL_DATA_SOURCES_PROPERTY), QMAIDataSource[].class);
        return dataSources == null ? List.of() : List.copyOf(Arrays.asList(dataSources));
    }

    @NotNull
    public Set<QMAIContextObject> getObjects() {
        return objects;
    }

}
