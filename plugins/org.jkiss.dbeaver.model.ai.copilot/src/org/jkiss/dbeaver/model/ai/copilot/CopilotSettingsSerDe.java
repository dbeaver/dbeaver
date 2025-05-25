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
package org.jkiss.dbeaver.model.ai.copilot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AIEngineSettingsSerDe;
import org.jkiss.dbeaver.model.ai.LegacyAISettings;

import java.lang.reflect.Type;

public class CopilotSettingsSerDe implements AIEngineSettingsSerDe<LegacyAISettings<CopilotProperties>> {
    private static final Type TYPE = new TypeToken<LegacyAISettings<CopilotProperties>>() {
    }.getType();

    @NotNull
    @Override
    public String getId() {
        return "copilot";
    }

    @NotNull
    @Override
    public JsonObject serialize(@NotNull AIEngineSettings configuration, Gson gson) {
        return gson.toJsonTree(configuration, TYPE).getAsJsonObject();
    }

    @NotNull
    @Override
    public LegacyAISettings<CopilotProperties> deserialize(@Nullable JsonObject jsonObject, Gson gson) {
        if (jsonObject == null) {
            return new LegacyAISettings<>(new CopilotProperties());
        }

        return gson.fromJson(jsonObject, TYPE);
    }
}
