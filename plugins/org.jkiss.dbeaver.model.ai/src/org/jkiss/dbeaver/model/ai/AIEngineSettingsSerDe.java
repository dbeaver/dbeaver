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
package org.jkiss.dbeaver.model.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface AIEngineSettingsSerDe<T extends AIEngineSettings<T>> {
    @NotNull
    String getId();

    /**
     * Serializes the given AI engine configuration into a JSON object.
     */
    @NotNull
    JsonObject serialize(@NotNull AIEngineSettings<T> configuration, Gson gson);

    /**
     * Deserializes the given JSON object into an AI engine configuration.
     * If the JSON object is null, a default configuration should be returned.
     */
    @NotNull
    T deserialize(@Nullable JsonObject jsonObject, Gson gson);
}
