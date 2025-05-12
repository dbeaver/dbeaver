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
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class LegacyAISettings<P extends AIEngineProperties> implements AIEngineSettings<LegacyAISettings<P>> {
    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create();

    @NotNull
    private final P properties;

    public LegacyAISettings(@NotNull P properties) {
        this.properties = properties;
    }

    @NotNull
    public P getProperties() {
        return properties;
    }

    @Override
    public void resolveSecrets() throws DBException {
        properties.resolveSecrets();
    }

    @Override
    public void saveSecrets() throws DBException {
        properties.saveSecrets();
    }

    @NotNull
    @Override
    public Map<String, Object> toMap() {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();

        return GSON.fromJson(GSON.toJson(properties), type);
    }

    @NotNull
    @Override
    public LegacyAISettings<P> merge(@NotNull Map<String, Object> map) {
        Map<String, Object> engineProperties = new HashMap<>(toMap());
        engineProperties.putAll(map);

        P newProperties = (P) GSON.fromJson(GSON.toJsonTree(engineProperties), properties.getClass());
        return new LegacyAISettings<>(newProperties);
    }
}
