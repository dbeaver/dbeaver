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
import org.jkiss.dbeaver.Log;

import java.util.List;

class AIChatSessionResponseConsumer implements AIChatResponseConsumer {
    private static final Log log = Log.getLog(AIChatSessionResponseConsumer.class);

    final StringBuilder response;
    private final AIChatSession chatSession;
    private final AIChatConversation conversation;

    public AIChatSessionResponseConsumer(
        @NotNull AIChatSession chatSession,
        @NotNull AIChatConversation conversation
    ) {
        this.chatSession = chatSession;
        this.conversation = conversation;
        this.response = new StringBuilder();
    }

    @Override
    public void nextMessageChunk(@NotNull String item) {
        response.append(item);
        List<AIChatMessage> messages = conversation.getMessages();
        if (!messages.isEmpty()) {
            chatSession.notifyMessageChunkReceived(conversation, messages.getLast(), item);
        } else {
            log.warn("Message chunk in empty conversation");
        }
    }

    @Override
    public void processFunctionCall(@NotNull AIMessage fcMessage) {
        addConversationMessage(fcMessage);
    }

    @Override
    public void warning(@NotNull String message) {
        addConversationMessage(
            AIMessage.warningMessage(message)
        );
    }

    @Override
    public void error(@NotNull Throwable throwable) {
        addConversationMessage(
            AIMessage.errorMessage(throwable)
        );
    }

    @Override
    public void complete(
        @NotNull List<AIMessageMeta> meta,
        boolean finishConversation
    ) {
        String assistantResponseText = response.toString();
        if (!assistantResponseText.isBlank()) {
            addConversationMessage(
                AIMessage.assistantMessage(assistantResponseText, meta)
            );
        }
        conversation.promptProcessed(finishConversation);
    }

    private void addConversationMessage(@NotNull AIMessage message) {
        AIChatMessage chatMessage = conversation.addMessage(message);
        chatSession.notifyMessageAdd(conversation, chatMessage);
    }

}
