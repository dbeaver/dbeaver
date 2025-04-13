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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;

public class AIConfigurationRegistry {
    public static final AIConfigurationRegistry INSTANCE = new AIConfigurationRegistry();

    private static final Log log = Log.getLog(AISettingsRegistry.class);
    public static final String AI_CONFIGURATION_JSON = "ai-configuration.json";
    private static final Gson saveSecuredPropsGson = PropertySerializationUtils.baseSecurePropertiesGsonBuilder()
        .create();
    private static final Gson saveNonSecurePropsGson = PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder()
        .create();

    public void save(AIConfiguration configuration) {
        String nonSecureProps = saveNonSecurePropsGson.toJson(configuration);
        String secureProps = saveSecuredPropsGson.toJson(configuration);
    }
}
