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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.*;

import java.util.List;
import java.util.Map;

public class AIAgentInternalDescriptor implements AIAgentDescriptor {

    private final AIFunctionInternalRegistry functionRegistry;

    public AIAgentInternalDescriptor() {
        this.functionRegistry = new AIFunctionInternalRegistry(this);
    }

    @Override
    @NotNull
    public String getAgentId() {
        return AIConstants.INTERNAL_AGENT_ID;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "DBeaver";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "DBeaver internal AI agent";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isAccessible() {
        return true;
    }

    @NotNull
    @Override
    public List<AIFunctionDescriptor> getSupportedFunctions() {
        return functionRegistry.getAllFunctions(AIFunctionPurpose.ALL);
    }

    @Nullable
    @Override
    public AIFunctionDescriptor getFunctionById(@NotNull String id) {
        return functionRegistry.getFunction(id);
    }

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        return functionRegistry.callFunction(context, descriptor, arguments);
    }

}
