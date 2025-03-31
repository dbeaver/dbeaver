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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.completion.DAIChatMessage;
import org.jkiss.dbeaver.model.ai.completion.DAIChatRole;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public final class AIUtils {
    /**
     * Counts tokens in the given list of messages.
     *
     * @param messages list of messages
     * @return number of tokens
     */
    public static int countTokens(@NotNull List<DAIChatMessage> messages) {
        int count = 0;
        for (DAIChatMessage message : messages) {
            count += countContentTokens(message.content());
        }
        return count;
    }

    /**
     * Truncates messages to fit into the given number of tokens.
     *
     * @param chatMode  true if chat mode is enabled
     * @param messages  list of messages
     * @param maxTokens maximum number of tokens
     * @return list of truncated messages
     */
    @NotNull
    public static List<DAIChatMessage> truncateMessages(
        boolean chatMode,
        @NotNull List<DAIChatMessage> messages,
        int maxTokens
    ) {
        final List<DAIChatMessage> pending = new ArrayList<>(messages);
        final List<DAIChatMessage> truncated = new ArrayList<>();
        int remainingTokens = maxTokens - 20; // Just to be sure

        if (!pending.isEmpty()) {
            if (pending.get(0).role() == DAIChatRole.SYSTEM) {
                // Always append main system message and leave space for the next one
                DAIChatMessage msg = pending.remove(0);
                DAIChatMessage truncatedMessage = truncateMessage(msg, remainingTokens - 50);
                remainingTokens -= countContentTokens(truncatedMessage.content());
                truncated.add(msg);
            }
        }

        for (DAIChatMessage message : pending) {
            final int messageTokens = message.content().length();

            if (remainingTokens < 0 || messageTokens > remainingTokens) {
                // Exclude old messages that don't fit into given number of tokens
                if (chatMode) {
                    break;
                } else {
                    // Truncate message itself
                }
            }

            DAIChatMessage truncatedMessage = truncateMessage(message, remainingTokens);
            remainingTokens -= countContentTokens(truncatedMessage.content());
            truncated.add(truncatedMessage);
        }

        return truncated;
    }

    /**
     * 1 token = 2 bytes
     * It is sooooo approximately
     * We should use https://github.com/knuddelsgmbh/jtokkit/ or something similar
     */
    private static DAIChatMessage truncateMessage(DAIChatMessage message, int remainingTokens) {
        String content = message.content();
        int contentTokens = countContentTokens(content);
        if (remainingTokens > contentTokens) {
            return message;
        }

        String truncatedContent = removeContentTokens(content, contentTokens - remainingTokens);
        return new DAIChatMessage(message.role(), truncatedContent);
    }

    private static String removeContentTokens(String content, int tokensToRemove) {
        int charsToRemove = tokensToRemove * 2;
        if (charsToRemove >= content.length()) {
            return "";
        }
        return content.substring(0, content.length() - charsToRemove) + "..";
    }

    private static int countContentTokens(String content) {
        return content.length() / 2;
    }

    /**
     * Processes completion text.
     */
    @NotNull
    public static String processCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer mainObject,
        @NotNull String completionText,
        @NotNull IAIFormatter formatter,
        boolean isChatAPI
    ) {
        if (CommonUtils.isEmpty(completionText)) {
            return "";
        }

        if (!isChatAPI) {
            completionText = "SELECT " + completionText.trim() + ";";
        }

        return formatter.postProcessGeneratedQuery(monitor, mainObject, executionContext, completionText).trim();
    }
}
