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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * AI function registry
 */
public class AIToolboxRegistry implements AIToolboxManager {

    private static final Log log = Log.getLog(AIToolboxRegistry.class);
    public static final String TOOLS_CONFIG_FILE_NAME = "ai-tools.json";

    private final AIToolboxInternalDescriptor internalToolbox;
    private final Map<String, AIToolboxDescriptor> externalToolboxes = new LinkedHashMap<>();
    private AIFunctionSettings functionSettings;
    private final Map<String, AIFunctionCategoryDescriptor> categoriesById = new LinkedHashMap<>();

    public AIToolboxRegistry() {
        internalToolbox = new AIToolboxInternalDescriptor();
        try {
            List<AIToolboxDescriptor> toolboxes = readExternalToolboxes();
            for (AIToolboxDescriptor toolbox: toolboxes) {
                externalToolboxes.put(toolbox.getToolboxId(), toolbox);
            }
        } catch (DBException e) {
            log.error("Error loading MCP configuration", e);
        }

        // Load function settings
        try {
            String mcpConfig = DBWorkbench.getPlatform().getConfigurationController().loadConfigurationFile(TOOLS_CONFIG_FILE_NAME);
            if (!CommonUtils.isEmpty(mcpConfig)) {
                try (Reader configReader = new StringReader(mcpConfig)) {
                    functionSettings = JSONUtils.GSON.fromJson(configReader, AIFunctionSettings.class);
                } catch (IOException e) {
                    log.error("Error parsing tools configuration", e);
                }
            }
        } catch (DBException e) {
            log.error("Error reading tools configuration", e);
        }
        if (functionSettings == null) {
            functionSettings = new AIFunctionSettings();
        }

        for (IConfigurationElement el : Platform.getExtensionRegistry().getConfigurationElementsFor(
            AIFunctionInternalDescriptor.EXTENSION_ID)
        ) {
            if ("category".equals(el.getName())) {
                var cd = new AIFunctionCategoryDescriptor(el);
                categoriesById.put(cd.getId(), cd);
            }
        }
    }

    @NotNull
    @Override
    public AIFunctionSettings getFunctionSettings() {
        return functionSettings;
    }

    @Override
    public void saveFunctionSettings() throws DBCException {
        // Save function settings
        try {
            String config = JSONUtils.GSON.toJson(functionSettings);
            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(
                TOOLS_CONFIG_FILE_NAME, config);
        } catch (DBException e) {
            log.error("Error saving tools configuration", e);
        }
    }

    @Override
    @NotNull
    public List<AIToolbox> getAllToolboxes() {
        List<AIToolbox> toolboxes = new ArrayList<>(externalToolboxes.size() + 1);
        toolboxes.add(internalToolbox);
        toolboxes.addAll(externalToolboxes.values());
        return toolboxes;
    }

    @NotNull
    @Override
    public List<AIFunctionDescriptor> getAllFunctions(@NotNull AIFunctionPurpose purpose) {
        List<AIFunctionDescriptor> functions = new ArrayList<>(internalToolbox.getSupportedFunctions());
        for (AIToolbox toolbox : externalToolboxes.values()) {
            if (toolbox.isEnabled() && toolbox.isAccessible()) {
                functions.addAll(toolbox.getSupportedFunctions());
            }
        }
        return functions;
    }

    @Override
    @Nullable
    public AIToolboxDescriptor getToolbox(@NotNull String toolboxId) {
        if (AIConstants.INTERNAL_TOOLBOX_ID.equals(toolboxId)) {
            return internalToolbox;
        }
        AIToolboxDescriptor toolbox = externalToolboxes.get(toolboxId);
        if (toolbox == null) {
            log.debug("Toolbox '" + toolboxId + "' not found in registry");
        }
        return toolbox;
    }

    @Nullable
    @Override
    public AIFunctionDescriptor getFunctionByFullId(@NotNull String fullId) {
        int divPos = fullId.indexOf("_");
        if (divPos < 0) {
            log.debug("Wrong function full ID: " + fullId);
            return null;
        }
        String tbId = fullId.substring(0, divPos);
        AIToolboxDescriptor toolbox = getToolbox(tbId);
        if (toolbox == null) {
            log.debug("Toolbox '" + tbId + "' not found");
            return null;
        }
        String functionId = fullId.substring(divPos + 1);
        AIFunctionDescriptor function = toolbox.getFunctionById(functionId);
        if (function == null) {
            log.debug("Function '" + functionId + "' not found in toolbox '" + tbId + "'");
            return null;
        }
        return function;
    }

    @NotNull
    protected List<AIToolboxDescriptor> readExternalToolboxes() throws DBException {
        return Collections.emptyList();
    }


    @NotNull
    @Override
    public List<AIFunctionCategoryDescriptor> getAllCategories() {
        return new ArrayList<>(categoriesById.values());
    }

}
