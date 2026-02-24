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

import java.util.*;

/**
 * AI function registry
 */
public class AIAgentRegistry implements AIAgentManager {

    private static final Log log = Log.getLog(AIAgentRegistry.class);

    public static final String MCP_CONFIG_FILE_NAME = "mcp.json";
    private static final String TAG_MCP_SERVERS = "mcpServers";

    private final List<AIAgentInternalDescriptor> internalAgents = new ArrayList<>();
    private final Map<String, AIAgentDescriptor> externalAgents = new LinkedHashMap<>();

    public AIAgentRegistry() {
        IConfigurationElement[] extElements = Platform.getExtensionRegistry().getConfigurationElementsFor(AIAgentInternalDescriptor.EXTENSION_ID);
        for (IConfigurationElement el : extElements) {
            if ("agent".equals(el.getName())) {
                internalAgents.add(new AIAgentInternalDescriptor(el));
            }
        }
        try {
            List<AIAgentDescriptor> agents = readExternalAgents();
            for (AIAgentDescriptor agent: agents) {
                externalAgents.put(agent.getAgentId(), agent);
            }
        } catch (DBException e) {
            log.error("Error loading MCP configuration", e);
        }
    }

    @Override
    @NotNull
    public List<AIAgent> getAllAgents() {
        List<AIAgent> agents = new ArrayList<>(internalAgents);
        agents.addAll(externalAgents.values());
        return agents;
    }

    @Nullable
    @Override
    public AIFunctionDescriptor getFunctionById(@NotNull String id) {
        return null;
    }

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        return null;
    }

    @NotNull
    protected List<AIAgentDescriptor> readExternalAgents() throws DBException {
        return Collections.emptyList();
    }

}
