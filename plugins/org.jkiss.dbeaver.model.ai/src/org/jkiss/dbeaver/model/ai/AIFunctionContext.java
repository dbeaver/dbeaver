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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.engine.AIFunctionCall;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI function call context
 */
public class AIFunctionContext {

    @NotNull
    private final DBRProgressMonitor monitor;
    @Nullable
    private final AIDatabaseContext context;
    @NotNull
    private final AIPromptGenerator prompt;
    @NotNull
    private final List<AIMessage> promptMessages;
    private final List<AIFunctionCall> functionCalls = new ArrayList<>();

    public AIFunctionContext(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator prompt,
        @NotNull List<AIMessage> promptMessages
    ) {
        this.monitor = monitor;
        this.context = context;
        this.prompt = prompt;
        this.promptMessages = promptMessages;
    }

    @NotNull
    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    @Nullable
    public AIDatabaseContext getContext() {
        return context;
    }

    @NotNull
    public AIPromptGenerator getPrompt() {
        return prompt;
    }

    @NotNull
    public List<AIMessage> getPromptMessages() {
        return promptMessages;
    }

    public List<AIFunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void addFunctionCall(@NotNull AIFunctionCall functionCall) {
        this.functionCalls.add(functionCall);
    }
}
