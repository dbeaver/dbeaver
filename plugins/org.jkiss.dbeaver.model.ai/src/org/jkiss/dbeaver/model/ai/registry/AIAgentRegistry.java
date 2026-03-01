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
public class AIAgentRegistry implements AIAgentManager {

    private static final Log log = Log.getLog(AIAgentRegistry.class);
    public static final String TOOLS_CONFIG_FILE_NAME = "ai-agents.json";

    private final AIAgentInternalDescriptor internalAgent;
    private final Map<String, AIAgentDescriptor> externalAgents = new LinkedHashMap<>();
    private AIFunctionSettings functionSettings;
    private final Map<String, AIFunctionCategoryDescriptor> categoriesById = new LinkedHashMap<>();

    public AIAgentRegistry() {
        internalAgent = new AIAgentInternalDescriptor();
        try {
            List<AIAgentDescriptor> agents = readExternalAgents();
            for (AIAgentDescriptor agent: agents) {
                externalAgents.put(agent.getAgentId(), agent);
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
                    log.error("Error parsing agent tools configuration", e);
                }
            }
        } catch (DBException e) {
            log.error("Error reading agent tools configuration", e);
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
            log.error("Error saving agent tools configuration", e);
        }
    }

    @Override
    @NotNull
    public List<AIAgent> getAllAgents() {
        List<AIAgent> agents = new ArrayList<>(externalAgents.size() + 1);
        agents.add(internalAgent);
        agents.addAll(externalAgents.values());
        return agents;
    }

    @NotNull
    @Override
    public List<AIFunctionDescriptor> getAllFunctions(@NotNull AIFunctionPurpose purpose) {
        List<AIFunctionDescriptor> functions = new ArrayList<>(internalAgent.getSupportedFunctions());
        for (AIAgent agent : externalAgents.values()) {
            if (agent.isEnabled() && agent.isAccessible()) {
                functions.addAll(agent.getSupportedFunctions());
            }
        }
        return functions;
    }

    @Override
    @Nullable
    public AIAgentDescriptor getAgent(@NotNull String agentId) {
        if (AIConstants.INTERNAL_AGENT_ID.equals(agentId)) {
            return internalAgent;
        }
        AIAgentDescriptor agent = externalAgents.get(agentId);
        if (agent == null) {
            log.debug("Agent '" + agentId + "' not found in registry");
        }
        return agent;
    }

    @Nullable
    @Override
    public AIFunctionDescriptor getFunctionById(@NotNull String fullId) {
        String functionId;
        AIAgentDescriptor agent;
        int divPos = fullId.indexOf(':');
        if (divPos == -1) {
            agent = internalAgent;
            functionId = fullId;
        } else {
            agent = externalAgents.get(fullId.substring(0, divPos));
            if (agent == null) {
                return null;
            }
            functionId = fullId.substring(divPos + 1);
        }
        return agent.getFunctionById(functionId);
    }

    @NotNull
    protected List<AIAgentDescriptor> readExternalAgents() throws DBException {
        return Collections.emptyList();
    }


    @NotNull
    @Override
    public List<AIFunctionCategoryDescriptor> getAllCategories() {
        return new ArrayList<>(categoriesById.values());
    }

}
