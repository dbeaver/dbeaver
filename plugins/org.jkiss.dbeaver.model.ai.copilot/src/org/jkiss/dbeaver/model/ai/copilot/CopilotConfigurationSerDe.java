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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIEngineConfiguration;
import org.jkiss.dbeaver.model.ai.AIEngineConfigurationSerDe;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;

public class CopilotConfigurationSerDe implements AIEngineConfigurationSerDe<CopilotConfiguration> {
    private static final Gson readPropsGson = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create();
    private static final Gson saveNonSecurePropsGson = PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder()
        .create();

    @NotNull
    @Override
    public String getId() {
        return "copilot";
    }

    @NotNull
    @Override
    public JsonObject serialize(@NotNull AIEngineConfiguration configuration) {
        return saveNonSecurePropsGson.toJsonTree(configuration, CopilotConfiguration.class).getAsJsonObject();
    }

    @NotNull
    @Override
    public CopilotConfiguration deserialize(@Nullable JsonObject jsonObject) {
        if (jsonObject == null) {
            return new CopilotConfiguration();
        }

        return readPropsGson.fromJson(jsonObject, CopilotConfiguration.class);
    }
}
