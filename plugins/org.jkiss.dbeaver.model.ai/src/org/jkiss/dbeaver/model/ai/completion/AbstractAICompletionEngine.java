/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.completion;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractAICompletionEngine<SERVICE, REQUEST> implements DAICompletionEngine<SERVICE> {

    @NotNull
    @Override
    public List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull DAICompletionMessage message,
        @NotNull IAIFormatter formatter
    ) throws DBException {
        String result = requestCompletion(monitor, context, List.of(message), formatter, false);
        if (result == null) {
            return Collections.emptyList();
        }
        // Remove empty lines
        result = result.replaceAll("\n\n", "\n");

        DAICompletionResponse response = createCompletionResponse(context, result);
        return Collections.singletonList(response);
    }

    @NotNull
    @Override
    public List<DAICompletionResponse> performSessionCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull DAICompletionSession session,
        @NotNull IAIFormatter formatter,
        boolean includeAssistantMessages
    ) throws DBException {
        final DAICompletionResponse response = new DAICompletionResponse();
        response.setResultCompletion(requestCompletion(
            monitor,
            context,
            filterMessages(session.getMessages(), includeAssistantMessages),
            formatter,
            true
        ));
        return List.of(response);
    }

    @NotNull
    protected List<DAICompletionMessage> filterMessages(List<DAICompletionMessage> messages, boolean includeAssistantMessages ) {
        if (includeAssistantMessages) {
            return messages;
        }
        return messages.stream().filter(it -> DAICompletionMessage.Role.USER.equals(it.getRole())).toList();
    }

    public abstract Map<String, SERVICE> getServiceMap();

    protected abstract int getMaxTokens();

    /**
     * Provides instructions for the AI so it can generate more accurate completions
     *
     * @param chatCompletion if the completion is for the chat mode, or for a single completion request.
     */
    @NotNull
    protected String getInstructions(boolean chatCompletion) {
        return """
            You are SQL assistant. You must produce SQL code for given prompt.
            You must produce valid SQL statement enclosed with Markdown code block and terminated with semicolon.
            All database object names should be properly escaped according to the SQL dialect.
            All comments MUST be placed before query outside markdown code block.
            Be polite.
            """;
    }

    @Nullable
    abstract protected String requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull IAIFormatter formatter,
        boolean chatCompletion
    ) throws DBException;

    @NotNull
    protected DAICompletionResponse createCompletionResponse(
        @NotNull DAICompletionContext context,
        @NotNull String result
    ) {
        DAICompletionResponse response = new DAICompletionResponse();
        response.setResultCompletion(result);
        return response;
    }

    @Nullable
    protected abstract String callCompletion(
        @NotNull DBRProgressMonitor monitor,
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull SERVICE service,
        @NotNull REQUEST completionRequest
    ) throws DBException;

    @NotNull
    protected DBSObjectContainer getScopeObject(
        @NotNull DAICompletionContext context,
        @NotNull DBCExecutionContext executionContext
    ) {
        DAICompletionScope scope = context.getScope();
        DBSObjectContainer mainObject = null;
        DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
        if (contextDefaults != null) {
            switch (scope) {
                case CURRENT_SCHEMA:
                    if (contextDefaults.getDefaultSchema() != null) {
                        mainObject = contextDefaults.getDefaultSchema();
                    } else {
                        mainObject = contextDefaults.getDefaultCatalog();
                    }
                    break;
                case CURRENT_DATABASE:
                    mainObject = contextDefaults.getDefaultCatalog();
                    break;
                default:
                    break;
            }
        }
        if (mainObject == null) {
            mainObject = ((DBSObjectContainer) executionContext.getDataSource());
        }
        return mainObject;
    }

    protected abstract REQUEST createCompletionRequest(boolean chatMode, @NotNull List<DAICompletionMessage> messages) throws DBCException;

    protected abstract REQUEST createCompletionRequest(boolean chatMode, @NotNull List<DAICompletionMessage> messages, int maxTokens) throws DBCException;

    protected abstract SERVICE getServiceInstance(@NotNull DBCExecutionContext executionContext) throws DBException;

    protected abstract AIEngineSettings getSettings();

    @Nullable
    protected String processCompletion(
        @NotNull List<DAICompletionMessage> messages,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer mainObject,
        @Nullable String completionText,
        @NotNull IAIFormatter formatter,
        @NotNull boolean isChatAPI
    ) {
        if (CommonUtils.isEmpty(completionText)) {
            return null;
        }

        if (!isChatAPI) {
            completionText = "SELECT " + completionText.trim() + ";";
        }

        return formatter.postProcessGeneratedQuery(monitor, mainObject, executionContext, completionText).trim();
    }


    @NotNull
    protected static List<DAICompletionMessage> truncateMessages(
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        int maxTokens
    ) {
        final List<DAICompletionMessage> pending = new ArrayList<>(messages);
        final List<DAICompletionMessage> truncated = new ArrayList<>();
        int remainingTokens = maxTokens - 20; // Just to be sure

        if (!pending.isEmpty()) {
            if (pending.get(0).getRole() == DAICompletionMessage.Role.SYSTEM) {
                // Always append main system message and leave space for the next one
                DAICompletionMessage msg = pending.remove(0);
                remainingTokens -= truncateMessage(msg, remainingTokens - 50);
                truncated.add(msg);
            }
        }

        for (DAICompletionMessage message : pending) {
            final int messageTokens = message.getContent().length();

            if (remainingTokens < 0 || messageTokens > remainingTokens) {
                // Exclude old messages that don't fit into given number of tokens
                if (chatMode) {
                    break;
                } else {
                    // Truncate message itself
                }
            }

            remainingTokens -= truncateMessage(message, remainingTokens);
            truncated.add(message);
        }

        return truncated;
    }

    /**
     * 1 token = 2 bytes
     * It is sooooo approximately
     * We should use https://github.com/knuddelsgmbh/jtokkit/ or something similar
     */
    protected static int truncateMessage(DAICompletionMessage message, int remainingTokens) {
        String content = message.getContent();
        int contentTokens = countContentTokens(content);
        if (contentTokens > remainingTokens) {
            int tokensToRemove = contentTokens - remainingTokens;
            content = removeContentTokens(content, tokensToRemove);
            message.setContent(content);
            return contentTokens - tokensToRemove;
        }
        return contentTokens;
    }

    protected static String removeContentTokens(String content, int tokensToRemove) {
        int charsToRemove = tokensToRemove * 2;
        if (charsToRemove >= content.length()) {
            return "";
        }
        return content.substring(0, content.length() - charsToRemove) + "..";
    }

    protected static int countContentTokens(String content) {
        return content.length() / 2;
    }

}
