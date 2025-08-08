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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIQueryConfirmationRule;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AIUtils {
    /**
     * How many characters we roughly get from a single token.
     */
    public static final int TOKEN_TO_CHAR_RATIO = 2;

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
    public static int countTokens(@NotNull List<AIMessage> messages) {
        int count = 0;
        for (AIMessage message : messages) {
            count += countContentTokens(message.getContent());
        }
        return count;
    }

    /**
     * Truncates messages to fit into the given number of tokens.
     *
     * @param messages  list of messages
     * @param maxTokens maximum number of tokens
     * @return list of truncated messages
     */
    @NotNull
    public static List<AIMessage> truncateMessages(
        @NotNull List<AIMessage> messages,
        int maxTokens
    ) {
        final List<AIMessage> pending = new ArrayList<>(filterEmptyMessages(messages));
        final List<AIMessage> truncated = new ArrayList<>();
        int remainingTokens = maxTokens - 20; // Just to be sure

        if (pending.isEmpty()) {
            return truncated; // Nothing to truncate
        } else if (pending.size() == 1) {
            // If we have only one message, we can return it as is
            AIMessage singleMessage = pending.getFirst();
            if (countContentTokens(singleMessage.getContent()) <= remainingTokens) {
                return List.of(singleMessage);
            } else {
                return List.of(truncateMessage(singleMessage, remainingTokens));
            }
        } else if (pending.getFirst().getRole() == AIMessageType.SYSTEM) {
            // Always append main system message and leave space for the next one
            AIMessage msg = pending.removeFirst();
            AIMessage truncatedMessage = truncateMessage(msg, remainingTokens - 50);
            remainingTokens -= countContentTokens(truncatedMessage.getContent());
            truncated.add(msg);
        }

        for (AIMessage message : pending) {
            final int messageTokens = message.getContent().length();

            if (remainingTokens < 0 || messageTokens > remainingTokens) {
                break;
            }

            AIMessage truncatedMessage = truncateMessage(message, remainingTokens);
            remainingTokens -= countContentTokens(truncatedMessage.getContent());
            truncated.add(truncatedMessage);
        }

        return truncated;
    }

    /**
     * 1 token = 2 bytes
     * It is sooooo approximately
     * We should use https://github.com/knuddelsgmbh/jtokkit/ or something similar
     */
    private static AIMessage truncateMessage(AIMessage message, int remainingTokens) {
        String content = message.getContent();
        int contentTokens = countContentTokens(content);
        if (remainingTokens > contentTokens) {
            return message;
        }

        String truncatedContent = removeContentTokens(content, contentTokens - remainingTokens);
        return new AIMessage(message.getRole(), truncatedContent);
    }

    private static String removeContentTokens(String content, int tokensToRemove) {
        int charsToRemove = tokensToRemove * TOKEN_TO_CHAR_RATIO;
        if (charsToRemove >= content.length()) {
            return "";
        }
        return content.substring(0, content.length() - charsToRemove) + "..";
    }

    private static int countContentTokens(String content) {
        return content.length() / TOKEN_TO_CHAR_RATIO;
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
     * Retrieves the DDL for the given DBSObject if applicable.
     *
     * @param object  the DBSObject from which to retrieve the DDL
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
                    return scriptObject.getObjectDefinitionText(
                        monitor, Map.of(
                            DBPScriptObject.OPTION_INCLUDE_COMMENTS, false,
                            DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, false,
                            DBPScriptObject.OPTION_SKIP_INDEXES, true, // Exclude indexes
                            DBPScriptObject.OPTION_SKIP_DROPS, true // Exclude --DROP
                        )
                    );
                } catch (DBException e) {
                    log.debug(e);
                }
            }
        }
        return null;
    }

    public static boolean confirmExecutionIfNeeded(
        @NotNull List<SQLScriptElement> scriptElements,
        boolean isCommand
    ) {
        Set<SQLQueryCategory> queryCategories = SQLQueryCategory.categorizeScript(scriptElements);
        if (queryCategories.contains(SQLQueryCategory.DDL) && isConfirmationNeeded(AIConstants.AI_CONFIRM_DDL)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_ddl_message :
                AIMessages.ai_execute_query_confirm_ddl_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message);
        }
        if (queryCategories.contains(SQLQueryCategory.DML) && isConfirmationNeeded(AIConstants.AI_CONFIRM_DML)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_dml_message :
                AIMessages.ai_execute_query_confirm_dml_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message);
        }
        boolean isSqlOrUnknown = queryCategories.contains(SQLQueryCategory.SQL) ||
            queryCategories.contains(SQLQueryCategory.UNKNOWN);
        if (isSqlOrUnknown && isConfirmationNeeded(AIConstants.AI_CONFIRM_SQL)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_sql_message :
                AIMessages.ai_execute_query_confirm_sql_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message);
        }
        return true;
    }

    public static void disableAutoCommitIfNeeded(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<SQLScriptElement> scriptElements,
        @Nullable DBCExecutionContext context
    ) throws DBException {
        if (!SQLQueryCategory.categorizeScript(scriptElements).contains(SQLQueryCategory.DML)) {
            return;
        }

        AIQueryConfirmationRule dmlRule = CommonUtils.valueOf(
            AIQueryConfirmationRule.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.AI_CONFIRM_DML),
            AIQueryConfirmationRule.CONFIRM
        );
        if (dmlRule == AIQueryConfirmationRule.DISABLE_AUTOCOMMIT) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            if (txnManager != null && txnManager.isAutoCommit()) {
                txnManager.setAutoCommit(monitor, false);
                showAutoCommitDisabledNotification();
            }
        }
    }

    private static List<AIMessage> filterEmptyMessages(@NotNull List<AIMessage> messages) {
        return messages.stream()
            .filter(message -> !message.getContent().isBlank())
            .toList();
    }

    private static void showAutoCommitDisabledNotification() {
        DBWorkbench.getPlatformUI().showWarningNotification(
            AIMessages.ai_execute_query_auto_commit_disabled_title,
            AIMessages.ai_execute_query_auto_commit_disabled_message
        );
    }

    private static boolean isConfirmationNeeded(@NotNull String actionName) {
        return CommonUtils.valueOf(
            AIQueryConfirmationRule.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(actionName),
            AIQueryConfirmationRule.EXECUTE
        ) == AIQueryConfirmationRule.CONFIRM;
    }

    private static boolean confirmExecute(String title, String message) {
        return DBWorkbench.getPlatformUI().confirmAction(title, message, true);
    }
}
