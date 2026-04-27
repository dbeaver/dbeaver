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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI function call info
 */
public class AIFunctionCall {

    private static final Log log = Log.getLog(AIFunctionCall.class);

    @NotNull
    private String functionName;
    @Nullable
    private Map<String, Object> arguments;
    @Nullable
    private String hint;
    @Nullable
    private transient AIFunctionDescriptor function;

    /**
     * Properties received from AI engine. Can be required to pass down for further messages
     * Example: Anthropic requires passing tool_use_id for function results to work properly
     */
    @Nullable
    private Map<String, String> messageMetadata;

    public AIFunctionCall() {
        functionName = "";
        arguments = Map.of();
    }

    public AIFunctionCall(
        @NotNull String functionName,
        @NotNull Map<String, Object> arguments,
        @Nullable Map<String, String> messageMetadata
    ) {
        this.functionName = functionName;
        this.arguments = arguments;
        this.messageMetadata = messageMetadata;
    }

    public AIFunctionCall(@NotNull String functionName, @Nullable Map<String, Object> arguments) {
        this(functionName, arguments, null);
    }

    @NotNull
    public String getFunctionName() {
        if (function != null) {
            return function.getFullId();
        }
        return functionName;
    }

    public void setFunctionName(@NotNull String functionName) {
        this.functionName = functionName;
    }

    @NotNull
    public String getFunctionDisplayName() {
        if (function != null) {
            return function.getName();
        }
        return functionName;
    }

    @NotNull
    public Map<String, Object> getArguments() {
        return arguments != null ? arguments : Map.of();
    }

    public void setArguments(@NotNull Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    @Nullable
    public Map<String, String> getMessageMetadata() {
        return messageMetadata;
    }

    public void setMessageMetadata(@NotNull Map<String, String> messageMetadata) {
        this.messageMetadata = Map.copyOf(messageMetadata);
    }

    @Nullable
    public String getHint() {
        return hint;
    }

    public void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    @Nullable
    public AIFunctionDescriptor getFunction() {
        return function;
    }

     public void setFunction(@NotNull AIFunctionDescriptor function) {
        this.function = function;
    }

    @Override
    public String toString() {
        return functionName + "(" + arguments + ")";
    }

    @Nullable
    public AIFunctionDescriptor getOrResolveFunction(@NotNull AIToolboxManager tbm) {
        if (function == null) {
            function = tbm.getFunctionByFullId(functionName);
        }
        return function;
    }

    public void transformArguments(
        @Nullable AIDatabaseContext context,
        @NotNull AIFunctionContext functionContext
    ) {
        // In headless apps (server apps) we do not call action functions directly
        // We pass all parameters in the result
        if (function != null && arguments != null) {
            Map<String, Object> ta = new LinkedHashMap<>(arguments);
            for (Map.Entry<String, Object> arg : arguments.entrySet()) {
                String paramName = arg.getKey();
                AIFunctionParameter parameter = function.getParameter(paramName);
                if (parameter != null) {
                    AIFunctionParameterTransformer transformer = parameter.getTransformer();
                    if (transformer != null) {
                        try {
                            Object paramValue = arg.getValue();
                            Object transformedValue = transformer.transformParameter(
                                context,
                                functionContext,
                                function,
                                parameter,
                                paramValue
                            );
                            ta.put(paramName + "_" + parameter.getTransformerSuffix(), transformedValue);
                        } catch (Exception e) {
                            log.debug("Error transforming AI function parameter value", e);
                        }
                    }
                }
            }
            this.arguments = ta;
        }
    }
}
