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
package org.jkiss.dbeaver.model.ai.openai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AIEngineSettingsSerDe;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;

public class OpenAISettingsSerDe implements AIEngineSettingsSerDe<OpenAISettings> {
    private static final Gson readPropsGson = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create();
    private static final Gson saveNonSecurePropsGson = PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder()
        .create();

    @NotNull
    @Override
    public String getId() {
        return AIConstants.OPENAI_ENGINE;
    }

    @NotNull
    @Override
    public JsonObject serialize(@NotNull AIEngineSettings configuration) {
        return saveNonSecurePropsGson.toJsonTree(configuration, OpenAISettings.class).getAsJsonObject();
    }

    @NotNull
    @Override
    public OpenAISettings deserialize(@Nullable JsonObject jsonObject) {
        if (jsonObject == null) {
            return new OpenAISettings();
        }

        return readPropsGson.fromJson(jsonObject, OpenAISettings.class);
    }
}
