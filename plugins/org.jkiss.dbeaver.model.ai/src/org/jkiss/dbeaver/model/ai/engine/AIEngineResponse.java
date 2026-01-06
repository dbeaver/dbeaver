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

package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIMessageType;

import java.util.List;

/**
 * Completion request
 */
public class AIEngineResponse {
    @NotNull
    private final AIMessageType type;
    @Nullable
    private final List<String> variants;
    @Nullable
    private final AIFunctionCall functionCall;

    private int inputTokensConsumed;
    private int outputTokensConsumed;
    private int processingTime;

    /**
     * Constructs response with text message
     */
    public AIEngineResponse(
        @NotNull AIMessageType type,
        @NotNull List<String> variants
    ) {
        this.type = type;
        this.variants = variants;
        this.functionCall = null;
    }

    /**
     * Constructs response with function call
     */
    public AIEngineResponse(@NotNull AIFunctionCall functionCall) {
        this.type = AIMessageType.FUNCTION;
        this.variants = null;
        this.functionCall = functionCall;
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
        return functionCall;
    }

    public int getInputTokensConsumed() {
        return inputTokensConsumed;
    }

    public void setInputTokensConsumed(int inputTokensConsumed) {
        this.inputTokensConsumed = inputTokensConsumed;
    }

    public int getOutputTokensConsumed() {
        return outputTokensConsumed;
    }

    public void setOutputTokensConsumed(int outputTokensConsumed) {
        this.outputTokensConsumed = outputTokensConsumed;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }

    @Override
    public String toString() {
        return "AI response (" + type + ") " + (variants != null ? variants : functionCall);
    }

}
