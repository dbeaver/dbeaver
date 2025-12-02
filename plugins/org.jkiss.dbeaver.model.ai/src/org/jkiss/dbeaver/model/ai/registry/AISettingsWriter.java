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
package org.jkiss.dbeaver.model.ai.registry;

import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

public class AISettingsWriter {
    private static final Log log = Log.getLog(AISettingsWriter.class);

    private static final String AI_DISABLED_KEY = "aiDisabled";
    private static final String ACTIVE_ENGINE_KEY = "activeEngine";
    private static final String PROPERTIES_KEY = "properties";
    private static final String ENGINE_CONFIGURATIONS_KEY = "engineConfigurations";
    private static final String ENABLED_FUNCTION_CATEGORIES_KEY = "enabledFunctionCategories";
    private static final String ENABLED_FUNCTIONS_KEY = "enabledFunctions";
    private static final String ENGINE_PROPERTIES = "properties";

    private final String configurationFileName;
    private final Gson readPropsGson = createPropertiesLoadGson();
    private final Gson writePropsGson = createPropertiesSaveGson();

    public AISettingsWriter(String configurationFileName) {
        this.configurationFileName = configurationFileName;
    }

    @NotNull
    public AISettings readSettings() {
        Map<String, Object> configMap = null;
        try {
            String content = loadConfig();
            if (!CommonUtils.isEmpty(content)) {
                configMap = readPropsGson.fromJson(new StringReader(content), JSONUtils.MAP_TYPE_TOKEN);
            }
        } catch (Exception e) {
            log.error("Error loading AI settings, falling back to defaults.", e);
        }
        if (configMap == null) {
            configMap = new LinkedHashMap<>();
        }

        AISettings settings = new AISettings(readPropsGson, writePropsGson);

        if (!configMap.isEmpty()) {
            settings.setAiDisabled(JSONUtils.getBoolean(configMap, AI_DISABLED_KEY));
            settings.setActiveEngine(JSONUtils.getString(configMap, ACTIVE_ENGINE_KEY));
            JSONUtils.getObject(configMap, PROPERTIES_KEY).forEach(settings::setProperty);

            List<String> enabledCategories = JSONUtils.getStringList(configMap, ENABLED_FUNCTION_CATEGORIES_KEY);
            if (!enabledCategories.isEmpty()) {
                settings.setEnabledFunctionCategories(new HashSet<>(enabledCategories));
            }

            List<String> enabledFunctions = JSONUtils.getStringList(configMap, ENABLED_FUNCTIONS_KEY);
            if (!enabledFunctions.isEmpty()) {
                settings.setEnabledFunctions(new HashSet<>(enabledFunctions));
            }

            Map<String, JsonElement> engineConfigElements = JSONUtils.getObject(configMap, ENGINE_CONFIGURATIONS_KEY).entrySet()
                .stream()
                .map(it -> {
                    if (it.getValue() instanceof Map map) {
                        Map<String, Object> properties = JSONUtils.getObject(map, ENGINE_PROPERTIES);
                        JsonElement engineConfigTree = readPropsGson.toJsonTree(properties, Map.class);
                        return Map.entry(it.getKey(), engineConfigTree);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            settings.setRawEngineConfigurations(engineConfigElements);

            if (settings.getEnabledFunctionCategories().isEmpty()) {
                settings.setEnabledFunctionCategories(
                    AIFunctionRegistry.getInstance().getDefaultEnabledCategoryIds()
                );
            }
        }
        if (settings.activeEngine() == null || !settings.hasConfiguration(settings.activeEngine())) {
            settings.setActiveEngine(OpenAIConstants.OPENAI_ENGINE);
        }

        return settings;
    }

    public void writeSettings(@NotNull AISettings settings) throws DBException {
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            log.warn("The user has no permission to save AI configuration");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty(AI_DISABLED_KEY, settings.isAiDisabled());
        json.addProperty(ACTIVE_ENGINE_KEY, settings.activeEngine());

        JsonObject propertiesObject = new JsonObject();
        for (Map.Entry<String, Object> property : settings.getAllProperties().entrySet()) {
            JsonElement propValue = writePropsGson.toJsonTree(property.getValue());
            propertiesObject.add(property.getKey(), propValue);
        }
        json.add(PROPERTIES_KEY, propertiesObject);

        Set<String> enabledCategories = settings.getEnabledFunctionCategories();
        if (!enabledCategories.isEmpty()) {
            JsonArray categoriesArray = new JsonArray();
            for (String category : enabledCategories) {
                categoriesArray.add(category);
            }
            json.add(ENABLED_FUNCTION_CATEGORIES_KEY, categoriesArray);
        }

        Set<String> enabledFunctions = settings.getEnabledFunctions();
        if (!enabledFunctions.isEmpty()) {
            JsonArray functionsArray = new JsonArray();
            for (String function : enabledFunctions) {
                functionsArray.add(function);
            }
            json.add(ENABLED_FUNCTIONS_KEY, functionsArray);
        }


        JsonObject engineConfigurations = new JsonObject();
        for (Map.Entry<String, JsonElement> configuration : settings.getEngineConfigurationsRaw().entrySet()) {
            if (configuration.getValue() instanceof JsonObject jo && !jo.isEmpty()) {
                JsonObject props = new JsonObject();
                props.add(ENGINE_PROPERTIES, jo);
                engineConfigurations.add(configuration.getKey(), props);
            }
        }
        json.add(ENGINE_CONFIGURATIONS_KEY, engineConfigurations);

        String content = writePropsGson.toJson(json);

        DBWorkbench.getPlatform().getConfigurationController()
            .saveConfigurationFile(configurationFileName, content);

        if (!saveSecretsAsPlainText()) {
            settings.saveSecrets();
        }
    }


    @Nullable
    private String loadConfig() throws DBException {
        return DBWorkbench.getPlatform()
            .getConfigurationController()
            .loadConfigurationFile(configurationFileName);
    }

    public boolean isConfigExists() throws DBException {
        String content = loadConfig();
        return CommonUtils.isNotEmpty(content);
    }

    public static boolean saveSecretsAsPlainText() {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        return application.isMultiuser() || application.isDistributed();
    }


    @NotNull
    private Gson createPropertiesLoadGson() {
        return new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create();
    }

    @NotNull
    private Gson createPropertiesSaveGson() {
        if (saveSecretsAsPlainText()) {
            return createPropertiesLoadGson();
        } else {
            return PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder().create();
        }
    }

}
