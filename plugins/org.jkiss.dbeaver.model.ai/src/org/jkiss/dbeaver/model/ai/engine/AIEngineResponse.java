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

package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIUsage;

import java.util.List;
import java.util.Map;

/**
 * Completion request
 */
public class AIEngineResponse {
    @NotNull
    private final AIMessageType type;
    @Nullable
    private final List<String> variants;
    @NotNull
    private final List<AIFunctionCall> functionCalls;
    @Nullable
    private final AIUsage usage;
    private int processingTime;
    @NotNull
    private Map<String, String> messageMetadata = Map.of();

    @NotNull
    public Map<String, String> getMessageMetadata() {
        return messageMetadata;
    }

    public void setMessageMetadata(@NotNull Map<String, String> messageMetadata) {
        this.messageMetadata = Map.copyOf(messageMetadata);
    }

    /**
     * Constructs response with text message
     */
    public AIEngineResponse(
        @NotNull AIMessageType type,
        @NotNull List<String> variants,
        @Nullable AIUsage usage
    ) {
        this.type = type;
        this.variants = variants;
        this.usage = usage;
        this.functionCalls = List.of();
    }

    /**
     * Constructs response with function call
     */
    public AIEngineResponse(@NotNull AIFunctionCall functionCall, @Nullable AIUsage usage) {
        this(List.of(functionCall), usage);
    }

    public AIEngineResponse(@NotNull List<AIFunctionCall> functionCalls, @Nullable AIUsage usage) {
        this.usage = usage;
        this.type = AIMessageType.FUNCTION;
        this.variants = null;
        this.functionCalls = List.copyOf(functionCalls);
    }

    @NotNull
    public AIMessageType getType() {
        return type;
    }

    @Nullable
    public List<String> getVariants() {
        return variants;
    }

    @Nullable
    public AIFunctionCall getFunctionCall() {
        return functionCalls.isEmpty() ? null : functionCalls.getFirst();
    }

    @NotNull
    public List<AIFunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    @Nullable
    public AIUsage getUsage() {
        return usage;
    }

    @Override
    @NotNull
    public String toString() {
        return "AI response (" + type + ") " + (variants != null ? variants : functionCalls);
    }

}
