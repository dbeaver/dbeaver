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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private List<AIFunctionCall> functionCalls = new ArrayList<>();
    @Nullable
    private AIChatConversation conversation;
    @Nullable
    private DatabaseContextProvider databaseContextProvider;

    @FunctionalInterface
    public interface DatabaseContextProvider {
        @Nullable
        AIDatabaseContext getContext(@Nullable String connectionId) throws DBException;
    }

    public AIFunctionContext(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator prompt
    ) {
        this.monitor = monitor;
        this.context = context;
        this.prompt = prompt;
    }

    public AIFunctionContext(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator prompt,
        @NotNull AIChatConversation conversation,
        @NotNull DatabaseContextProvider databaseContextProvider
    ) {
        this(monitor, context, prompt);
        this.conversation = conversation;
        this.databaseContextProvider = databaseContextProvider;
    }

    @Nullable
    public AIChatConversation getConversation() {
        return conversation;
    }

    @NotNull
    public AIFunctionContext forFunction(
        @NotNull AIFunctionDescriptor function,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        Object connectionId = arguments.get(AIFunctionParams.CONNECTION_ID_PARAM);
        if (connectionId != null && !(connectionId instanceof String)) {
            throw new DBException("Connection ID must be a string");
        }
        AIFunctionContext selected = function.getParameter(AIFunctionParams.CONNECTION_ID_PARAM) != null || connectionId != null
            ? withConnection((String) connectionId) : this;
        // validate both endpoints before any source data is read
        selected.withConnectionParameter(arguments, AIFunctionParams.SOURCE_CONNECTION_ID_PARAM);
        selected.withConnectionParameter(arguments, AIFunctionParams.TARGET_CONNECTION_ID_PARAM);
        return selected;
    }

    @NotNull
    public AIFunctionContext withConnectionParameter(@NotNull Map<String, Object> arguments, @NotNull String parameterName)
        throws DBException {
        Object connectionId = arguments.get(parameterName);
        if (connectionId == null) {
            return this;
        }
        if (!(connectionId instanceof String id)) {
            throw new DBException("Connection ID must be a string");
        }
        return withConnection(id);
    }

    @NotNull
    public AIFunctionContext forParameter(
        @NotNull AIFunctionDescriptor function,
        @NotNull Map<String, Object> arguments,
        @NotNull String parameterName
    ) throws DBException {
        if (parameterName.startsWith("source") && function.getParameter(AIFunctionParams.SOURCE_CONNECTION_ID_PARAM) != null) {
            return withConnectionParameter(arguments, AIFunctionParams.SOURCE_CONNECTION_ID_PARAM);
        }
        if (parameterName.startsWith("target") && function.getParameter(AIFunctionParams.TARGET_CONNECTION_ID_PARAM) != null) {
            return withConnectionParameter(arguments, AIFunctionParams.TARGET_CONNECTION_ID_PARAM);
        }
        return this;
    }

    @NotNull
    public AIFunctionContext withConnection(@Nullable String connectionId) throws DBException {
        AIDatabaseContext selectedContext;
        if (databaseContextProvider != null) {
            selectedContext = databaseContextProvider.getContext(connectionId);
        } else {
            if (!CommonUtils.isEmptyTrimmed(connectionId) && (context == null ||
                !connectionId.equals(context.getDataSource().getDataSourceContainer().getId()))) {
                throw new DBException("Connection '" + connectionId + "' is not available. Access restricted.");
            }
            selectedContext = context;
        }
        AIFunctionContext selected = new AIFunctionContext(monitor, selectedContext, prompt);
        selected.conversation = conversation;
        selected.databaseContextProvider = databaseContextProvider;
        selected.functionCalls = functionCalls;
        return selected;
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
    public List<AIFunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void addFunctionCall(@NotNull AIFunctionCall functionCall) {
        this.functionCalls.add(functionCall);
    }
}
