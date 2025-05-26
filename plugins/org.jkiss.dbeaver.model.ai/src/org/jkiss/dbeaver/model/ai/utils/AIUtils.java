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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.completion.DAIChatMessage;
import org.jkiss.dbeaver.model.ai.completion.DAIChatRole;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AIUtils {

    private static final Log log = Log.getLog(AIUtils.class);

    /**
     * Retrieves a secret value from the global secret controller.
     * If the secret value is empty, it returns the provided default value.
     */
    public static String getSecretValueOrDefault(
        @NotNull String secretId,
        @Nullable String defaultValue
    ) throws DBException {
        String secretValue = DBSSecretController.getGlobalSecretController().getPrivateSecretValue(secretId);
        if (CommonUtils.isEmpty(secretValue)) {
            return defaultValue;
        }

        return secretValue;
    }

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

    /**
     * Checks if the given DBPObject is eligible for AI description.
     *
     * @param dbpObject the object to check
     * @return true if the object can be described by AI, false otherwise
     */
    public static boolean isEligible(@Nullable DBPObject dbpObject) {
        if (dbpObject instanceof DataSourceDescriptor descriptor) {
            return descriptor.getDriver().isEmbedded();
        }
        return dbpObject instanceof DBSEntity
            || dbpObject instanceof DBSSchema
            || dbpObject instanceof DBSTableColumn
            || dbpObject instanceof DBSProcedure
            || dbpObject instanceof DBSTrigger
            || dbpObject instanceof DBSEntityConstraint;
    }

    /**
     * Computes the maximum number of tokens available for a request based on the engine's context size.
     *
     * @param engine the completion engine
     * @param monitor the progress monitor
     */
    public static int getMaxRequestTokens(@NotNull DAICompletionEngine engine, @NotNull DBRProgressMonitor monitor) throws DBException {
        return engine.getMaxContextSize(monitor) - AIConstants.MAX_RESPONSE_TOKENS;
    }

    /**
     * Retrieves the DDL for the given DBSObject if applicable.
     *
     * @param object the DBSObject from which to retrieve the DDL
     * @param monitor the progress monitor
     */
    public static String getObjectDDL(@Nullable DBSObject object, @NotNull DBRProgressMonitor monitor) {
        if (object instanceof DBSProcedure
            || object instanceof DBSTrigger
            || object instanceof DBSEntityConstraint
            || object instanceof DBSView
        ) {
            if (object instanceof DBPScriptObject scriptObject) {
                try {
                    return scriptObject.getObjectDefinitionText(monitor, Map.of());
                } catch (DBException e) {
                    log.debug(e);
                }
            }
        }
        return null;
    }
}
