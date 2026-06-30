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
package org.jkiss.dbeaver.model.ai.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptGenerateSql;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorRegistry;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class QMAIChatHistoryMapper {

    private static final Log log = Log.getLog(QMAIChatHistoryMapper.class);

    @NotNull
    public static QMAIConversationHistory toQMAIChatHistory(
        @NotNull AIChatConversation conversation,
        @Nullable AIContextSettings contextSettings,
        @Nullable AIDatabaseContext context
    ) {
        List<QMAIChatMessage> chatMessages = toQMAIChatMessages(conversation.getMessages());
        DBPDataSourceContainer chatDataSource = conversation.getDataSource();
        QMAIDataSource dataSource = chatDataSource == null
            ? null
            : new QMAIDataSource(chatDataSource.getProject().getId(), chatDataSource.getId());
        String contextJson = contextSettings == null ? null : contextSettings.saveSettingsToString();
        QMAIContext qmaiContext = new QMAIContext(contextJson, (context == null ? Set.of() : toQMAIContextObjects(context)));

        return new QMAIConversationHistory(
            conversation.getId().toString(),
            conversation.getCaption(),
            conversation.getPromptGenerator().generatorId(),
            dataSource,
            chatMessages,
            qmaiContext,
            conversation.getNextMessageId(),
            false
        );
    }

    @NotNull
    public static AIChatConversation toAIChatConversation(
        @NotNull AIAssistant assistant,
        @NotNull QMAIConversationHistory history,
        @Nullable DBPDataSourceContainer container
    ) {
        AIPromptGenerator generator = null;
        {
            String promptGeneratorId = history.getPromptGeneratorId();
            if (promptGeneratorId != null) {
                AIPromptGeneratorDescriptor generatorDescriptor = AIPromptGeneratorRegistry.getInstance()
                    .getPromptGenerator(promptGeneratorId);
                if (generatorDescriptor != null) {
                    try {
                        generator = generatorDescriptor.createGenerator();
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
            }
            if (generator == null) {
                log.trace("AI prompt generator '" + promptGeneratorId + "' not found. Use default");
                generator = new AIPromptGenerateSql();
            }
        }

        return new AIChatConversation(
            UUID.fromString(history.getId()),
            history.getCaption(),
            generator,
            toAIMessages(assistant, history.getMessages()),
            container,
            history.getNextMessageId()
        );
    }

    @NotNull
    public static List<QMAIChatMessage> toQMAIChatMessages(@NotNull List<AIChatMessage> messages) {
        return messages.stream().filter(it -> toQMAIChatRole(it.message().getRole()) != null).map(it -> {
            String content = it.message().getContent();
            return new QMAIChatMessage(
                it.id(),
                content,
                it.message().getRawDisplayMessage(),
                Objects.requireNonNull(toQMAIChatRole(it.message().getRole()), "Chat role is null"),
                getFunctionCallsString(it.message()),
                toJsonString(it.message().getFunctionResult()),
                it.message().getTime().toInstant(ZoneOffset.UTC),
                false,
                toQMAIMessageMeta(it.message().getMeta())
            );
        }).toList();
    }

    @Nullable
    private static String getFunctionCallsString(@NotNull AIMessage message) {
        if (message.getConfirmation() instanceof AIFunctionCallConfirmation fcc) {
            return toJsonString(fcc);
        }
        return toJsonString(message.getFunctionCall());
    }

    @Nullable
    private static String toJsonString(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        return JSONUtils.GSON.toJson(object);
    }

    @Nullable
    private static QMAIChatRole toQMAIChatRole(@NotNull AIMessageType role) {
        return switch (role) {
            case ASSISTANT -> QMAIChatRole.ASSISTANT;
            case CONFIRMATION -> QMAIChatRole.CONFIRMATION;
            case FUNCTION -> QMAIChatRole.FUNCTION;
            case WARNING -> QMAIChatRole.WARNING;
            case ERROR -> QMAIChatRole.ERROR;
            case USER -> QMAIChatRole.USER;
            default -> null;
        };
    }

    @NotNull
    public static List<AIChatMessage> toAIMessages(
        @NotNull AIAssistant assistant,
        @NotNull List<QMAIChatMessage> messages
    ) {
        return messages.stream().map(it -> {
            AIMessageType role = CommonUtils.valueOf(AIMessageType.class, it.role().name(), AIMessageType.USER);
            String content = it.content();
            String fcString = it.functionCall();
            String frString = it.functionResult();
            List<AIMessageMeta> messageMetas = toAIMessageMeta(it.meta());
            LocalDateTime messageTime = LocalDateTime.ofInstant(it.timestamp(), ZoneId.systemDefault());
            AIMessage aiMessage;
            if (!CommonUtils.isEmpty(fcString)) {
                if (role == AIMessageType.CONFIRMATION) {
                    AIFunctionCallConfirmation fcc = JSONUtils.GSON.fromJson(fcString, AIFunctionCallConfirmation.class);
                    for (AIFunctionCall fc : fcc.getFunctionCalls()) {
                        setFunctionToFunctionCall(assistant, fc);
                    }
                    aiMessage = new AIMessage(fcc, messageTime);
                } else {
                    AIFunctionCall fc = JSONUtils.GSON.fromJson(fcString, AIFunctionCall.class);
                    setFunctionToFunctionCall(assistant, fc);
                    AIFunctionResult fr = JSONUtils.GSON.fromJson(frString, AIFunctionResult.class);
                    aiMessage = new AIMessage(fc, fr, messageTime, messageMetas);
                }
            } else {
                aiMessage = new AIMessage(
                    role,
                    content,
                    it.displayMessage(),
                    messageTime,
                    messageMetas
                );
            }

            return new AIChatMessage(it.id(), aiMessage);
        })
        .toList();
    }

    private static void setFunctionToFunctionCall(@NotNull AIAssistant assistant, @NotNull AIFunctionCall fc) {
        AIFunctionDescriptor function = assistant.getToolboxManager().getFunctionByFullId(fc.getFunctionName());
        if (function != null) {
            fc.setFunction(function);
        }
    }

    @NotNull
    public static Set<QMAIContextObject> toQMAIContextObjects(
        @NotNull AIDatabaseContext context
    ) {
        switch (context.getScope()) {
            case CURRENT_DATASOURCE -> {
                return Set.of(
                    new QMAIContextObject(
                        context.getDataSource().getName(),
                        QMAIContextObjectType.DATASOURCE
                    )
                );
            }
            case CURRENT_DATABASE -> {
                return Set.of(
                    new QMAIContextObject(
                        context.getScopeObject().getName(),
                        QMAIContextObjectType.DATABASE
                    )
                );
            }
            case CURRENT_SCHEMA -> {
                return Set.of(
                    new QMAIContextObject(
                        context.getScopeObject().getName(),
                        QMAIContextObjectType.SCHEMA
                    )
                );
            }
            case CUSTOM -> {
                return context.getCustomEntities() == null ? Set.of() :
                    context.getCustomEntities().stream()
                        .filter(Objects::nonNull)
                        .map(entity -> new QMAIContextObject(
                            DBUtils.getObjectFullName(entity, DBPEvaluationContext.DDL),
                            defineObjectType(entity)
                        )).collect(Collectors.toSet());
            }
            default ->
                throw new IllegalArgumentException("Unsupported scope: " + context.getScope());
        }
    }

    @NotNull
    public static QMAIContextObjectType defineObjectType(@NotNull DBSObject object) {
        return switch (object) {
            case DBSLogicalDataSource ignored -> QMAIContextObjectType.DATASOURCE;
            case DBSObjectContainer ignored -> QMAIContextObjectType.SCHEMA;
            case DBPDataSource ignored -> QMAIContextObjectType.DATABASE;
            default -> QMAIContextObjectType.TABLE;
        };
    }

    @Nullable
    public static List<QMAIMessageMeta> toQMAIMessageMeta(@Nullable List<AIMessageMeta> meta) {
        if (meta == null) {
            return null;
        }
        return meta.stream()
            .filter(it -> it.usage() != null)
            .map(QMAIChatHistoryMapper::toQMAIMessageMeta)
            .toList();
    }

    @NotNull
    public static QMAIMessageMeta toQMAIMessageMeta(@NotNull AIMessageMeta meta) {
        return new QMAIMessageMeta(
            meta.type(),
            meta.engineId(),
            meta.modelId(),
            meta.systemPromptLength(),
            meta.timeSpent(),
            meta.usage().totalInputTokens(),
            meta.usage().cachedTokens(),
            meta.usage().totalOutputTokens(),
            meta.usage().reasoningTokens()
        );
    }

    @Nullable
    public static List<AIMessageMeta> toAIMessageMeta(@Nullable List<QMAIMessageMeta> meta) {
        if (meta == null) {
            return null;
        }

        return meta.stream()
            .map(QMAIChatHistoryMapper::toAIMessageMeta)
            .toList();
    }

    @NotNull
    public static AIMessageMeta toAIMessageMeta(@NotNull QMAIMessageMeta meta) {
        return new AIMessageMeta(
            meta.type(),
            meta.engineId(),
            meta.modelId(),
            new AIUsage(
                meta.totalInputTokens(),
                meta.cachedTokens(),
                meta.totalOutputTokens(),
                meta.reasoningTokens()
            ),
            meta.timeSpent(),
            meta.systemPromptLength()
        );
    }
}
