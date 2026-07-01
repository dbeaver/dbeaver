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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptGenerateSql;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI chat conversation.
 */
public class AIChatConversation {


    public enum State {
        NONE, // Initial state
        STARTED, // Processing prompt
        IDLE, // Waiting for function calls
        FINISHED, // Finished
        CANCELED, // Canceled
    }

    private static final int MAX_CAPTION_LENGTH = 64;
    private static final int PENDING_MESSAGE_ID = -1;

    @NotNull
    private final UUID uuid;
    @NotNull
    private String caption;
    @NotNull
    private final AIPromptGenerator promptGenerator;
    @NotNull
    private final List<AIChatMessage> messages;
    @Nullable
    private final DBPDataSourceContainer container;
    @NotNull
    private final LocalDateTime time;
    @Nullable
    private AIChatConversationSettings customSettings;

    /**
     * Sequence number of the next message.
     */
    private int nextMessageId;
    private State state = State.NONE;
    private CompletableFuture<AIChatConversation> finishFuture;

    public AIChatConversation(
        @NotNull String caption,
        @NotNull AIPromptGenerator promptGenerator,
        @NotNull List<AIChatMessage> messages,
        @Nullable DBPDataSourceContainer container
    ) {
        this(UUID.randomUUID(), caption, promptGenerator, messages, container, 0);
    }

    public AIChatConversation(
        @NotNull String caption,
        @NotNull AIPromptGenerator promptGenerator,
        @Nullable DBPDataSourceContainer container
    ) {
        this(caption, promptGenerator, List.of(), container);
    }

    public AIChatConversation(
        @NotNull UUID uuid,
        @NotNull String caption,
        @NotNull AIPromptGenerator promptGenerator,
        @NotNull List<AIChatMessage> messages,
        @Nullable DBPDataSourceContainer container,
        int nextMessageId
    ) {
        this.uuid = uuid;
        this.caption = StringUtils.truncateToSpace(caption, MAX_CAPTION_LENGTH);
        this.promptGenerator = promptGenerator;
        this.container = container;
        this.messages = new ArrayList<>(messages);
        this.time = messages.isEmpty() ? LocalDateTime.now() : messages.getLast().message().getTime();
        this.nextMessageId = nextMessageId;
    }

    @NotNull
    public UUID getId() {
        return uuid;
    }

    @NotNull
    public String getCaption() {
        return caption;
    }

    public void setCaption(@NotNull String caption) {
        this.caption = StringUtils.truncateToSpace(caption, MAX_CAPTION_LENGTH);
    }

    @NotNull
    public AIPromptGenerator getPromptGenerator() {
        return promptGenerator;
    }

    @NotNull
    public List<AIChatMessage> getMessages() {
        return messages.stream().filter(message -> !message.pending()).toList();
    }

    @Nullable
    public AIChatMessage getMessage(@NotNull String messageId) {
        int intId = CommonUtils.toInt(messageId);
        for (int i = messages.size(); i > 0; i--) {
            AIChatMessage message = messages.get(i - 1);
            if (!message.pending() && message.id() == intId) {
                return message;
            }
        }
        return null;
    }

    @NotNull
    public AIChatMessage addMessage(@NotNull AIMessage message) {
        if (getMessages().isEmpty() && promptGenerator instanceof AIPromptGenerateSql) {
            // First message - set conversation title
            String newCaption;
            String displayMessage = message.getRawDisplayMessage();
            if (!CommonUtils.isEmpty(displayMessage)) {
                newCaption = displayMessage;
            } else {
                newCaption = message.getContent();
            }
            this.caption = StringUtils.truncateToSpace(newCaption, MAX_CAPTION_LENGTH);
        }
        AIChatMessage chatMessage = new AIChatMessage(nextMessageId, message);
        nextMessageId++;

        this.messages.add(chatMessage);

        return chatMessage;
    }

    public boolean removeMessage(@NotNull AIChatMessage message) {
        return this.messages.remove(message);
    }

    public void clearMessages() {
        this.messages.clear();
    }

    public void clearMessagesAfter(@NotNull AIChatMessage message) {
        int index = messages.indexOf(message);
        while (messages.size() > index) {
            messages.removeLast();
        }
    }

    public void addPendingDeclinedFunctionCallMessages(@NotNull List<AIMessage> messages) {
        for (AIMessage message : messages) {
            this.messages.add(new AIChatMessage(PENDING_MESSAGE_ID, message, true));
        }
    }

    @NotNull
    public List<AIMessage> getPendingDeclinedFunctionCallMessages() {
        return messages.stream()
            .filter(AIChatMessage::pending)
            .map(AIChatMessage::message)
            .toList();
    }

    public void clearPendingDeclinedFunctionCallMessages() {
        messages.removeIf(AIChatMessage::pending);
    }

    @NotNull
    public AIExtendedUsage computeUsage() {
        List<AIMessageMeta> messageMetas = getMessages().stream()
            .map(AIChatMessage::message)
            .map(AIMessage::getMeta)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();

        return AIExtendedUsage.from(messageMetas);
    }

    @Nullable
    public DBPDataSourceContainer getDataSource() {
        return container;
    }

    public int getNextMessageId() {
        return nextMessageId;
    }

    @NotNull
    public LocalDateTime getTime() {
        return time;
    }

    @Nullable
    public AIChatConversationSettings getCustomSettings() {
        return customSettings;
    }

    public void setCustomSettings(@Nullable AIChatConversationSettings customSettings) {
        this.customSettings = customSettings;
    }

    @NotNull
    public LocalDateTime getLastMessageTime() {
        List<AIChatMessage> messages = getMessages();
        return messages.isEmpty() ? LocalDateTime.MIN : messages.getLast().message().getTime();
    }

    public boolean isTemporary() {
        return getDataSource() != null && getDataSource().isTemporary();
    }

    @NotNull
    public State getState() {
        return state;
    }

    public boolean isActive() {
        return switch (state) {
            case IDLE, STARTED -> true;
            default -> false;
        };
    }

    @NotNull
    public CompletableFuture<AIChatConversation> startConversation() {
        if (this.finishFuture == null) {
            this.finishFuture = new CompletableFuture<>();
        }
        this.state = State.STARTED;
        return finishFuture;
    }

    public void promptProcessed(boolean finishConversation) {
        this.state = finishConversation ? State.FINISHED : State.IDLE;
        if (finishConversation && finishFuture != null) {
            this.finishFuture.complete(this);
            this.finishFuture = null;
        }
    }

    public void cancelConversation() {
        this.state = State.CANCELED;
        if (finishFuture != null) {
            this.finishFuture.cancel(true);
            this.finishFuture = null;
        }
    }

    @Override
    public String toString() {
        return "AIChatConversation[" +
            "uuid=" + uuid + ", " +
            "caption=" + caption + ", " +
            "promptGenerator=" + promptGenerator.generatorId() + ", " +
            "messages=" + messages + ", " +
            "container=" + container + ']';
    }

}
