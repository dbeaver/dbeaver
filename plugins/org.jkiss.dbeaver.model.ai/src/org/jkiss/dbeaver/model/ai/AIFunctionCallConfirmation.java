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

package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI function call confirmation
 */
public class AIFunctionCallConfirmation extends AIConfirmation {

    @NotNull
    private final List<AIFunctionCall> functionCalls;

    public AIFunctionCallConfirmation(@NotNull List<AIFunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    @NotNull
    public List<AIFunctionCall> getFunctionCalls() {
        return functionCalls;
    }
    
    @Override
    @NotNull
    public String getMessage() {
        return "Confirm tools: " + functionCalls.stream().map(AIFunctionCall::getFunctionDisplayName)
            .collect(Collectors.joining(","));
    }

    public boolean hasInformationFunctions(@NotNull AIToolboxManager toolboxManager) {
        return AIUtils.hasInformationFunctions(toolboxManager, functionCalls);
    }

    @NotNull
    public List<AIFunctionCall> getFunctionCallsToConfirm(@NotNull AIToolboxManager toolboxManager) {
        AIFunctionSettings functionSettings = toolboxManager.getFunctionSettings();
        List<AIFunctionCall> callsToConfirm = new ArrayList<>();
        for (AIFunctionCall functionCall : functionCalls) {
            AIFunctionDescriptor function = functionCall.getOrResolveFunction(toolboxManager);
            if (function == null) {
                callsToConfirm.add(functionCall);
                continue;
            }
            AIFunctionSettings.ToolboxSettings toolboxSettings = functionSettings.getToolboxSettings(function.getToolbox());
            if (toolboxSettings.getFunctionAllowMode(function) == AIFunctionAllowMode.ASK) {
                callsToConfirm.add(functionCall);
            }
        }
        return callsToConfirm;
    }
}
