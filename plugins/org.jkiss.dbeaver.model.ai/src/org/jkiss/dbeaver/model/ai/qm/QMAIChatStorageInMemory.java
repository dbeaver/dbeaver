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
import org.jkiss.dbeaver.DBException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QMAIChatStorageInMemory implements AIChatStorage {
    private final Map<String, Map<String, QMAIConversationHistory>> conversations = new ConcurrentHashMap<>();

    public QMAIChatStorageInMemory() {
    }

    @NotNull
    @Override
    public List<QMAIConversationHistory> findConversations(@NotNull String sessionId) throws DBException {
        return conversations.getOrDefault(sessionId, Map.of()).values().stream()
            .sorted((o1, o2) -> {
                if (o1.getMessages().isEmpty() && o2.getMessages().isEmpty()) {
                    return 0;
                } else if (o1.getMessages().isEmpty()) {
                    return 1; // o1 is older
                } else if (o2.getMessages().isEmpty()) {
                    return -1; // o2 is older
                } else {
                    return o1.getMessages().getLast().timestamp().compareTo(o2.getMessages().getLast().timestamp());
                }
            })
            .toList();
    }

    @Override
    public void saveConversation(@NotNull String sessionId, @NotNull QMAIConversationHistory chat) throws DBException {
        conversations.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
            .put(chat.getId(), chat);
    }

    @Override
    public void appendMessages(@NotNull String conversationId, @NotNull List<QMAIChatMessage> messages) throws DBException {
        for (Map<String, QMAIConversationHistory> sessionConversations : conversations.values()) {
            QMAIConversationHistory chat = sessionConversations.get(conversationId);
            if (chat != null) {
                ArrayList<QMAIChatMessage> qmaiChatMessages = new ArrayList<>(chat.getMessages());
                qmaiChatMessages.addAll(messages);
                chat.setMessages(qmaiChatMessages);
                return;
            }
        }
        throw new DBException("Conversation not found: " + conversationId);
    }

    @Override
    public void deleteMessage(@NotNull String conversationId, int messageId) throws DBException {
        for (Map<String, QMAIConversationHistory> sessionConversations : conversations.values()) {
            QMAIConversationHistory chat = sessionConversations.get(conversationId);
            if (chat != null) {
                List<QMAIChatMessage> messages = new ArrayList<>(chat.getMessages());
                messages.removeIf(message -> message.id() >= messageId);
                chat.setMessages(messages);
                return;
            }
        }
        throw new DBException("Conversation not found: " + conversationId);
    }

    @Override
    public void extendContext(@NotNull String conversationId, @NotNull Set<QMAIContextObject> extra) throws DBException {
        for (Map<String, QMAIConversationHistory> sessionConversations : conversations.values()) {
            QMAIConversationHistory chat = sessionConversations.get(conversationId);
            if (chat != null) {
                QMAIContext context = chat.getContext();
                Set<QMAIContextObject> currentObjects = new HashSet<>(context.getObjects());
                currentObjects.addAll(extra);

                chat.setContext(new QMAIContext(
                    context.getContextJson(),
                    currentObjects
                ));
                return;
            }
        }
        throw new DBException("Conversation not found: " + conversationId);
    }

    @Override
    public void deleteConversation(@NotNull String conversationId) throws DBException {
        for (Map<String, QMAIConversationHistory> sessionConversations : conversations.values()) {
            if (sessionConversations.remove(conversationId) != null) {
                return; // Conversation found and removed
            }
        }
        throw new DBException("Conversation not found: " + conversationId);
    }

    @Override
    public void renameConversation(@NotNull String conversationId, @NotNull String newName) throws DBException {
        for (Map<String, QMAIConversationHistory> sessionConversations : conversations.values()) {
            QMAIConversationHistory chat = sessionConversations.get(conversationId);
            if (chat != null) {
                chat.setCaption(newName);
                return;
            }
        }
        throw new DBException("Conversation not found: " + conversationId);
    }

    @NotNull
    @Override
    public List<QMAIMessageMeta> getConversationHistoryMeta(@NotNull UUID conversationId) throws DBException {
        return List.of();
    }

    @NotNull
    @Override
    public List<QMAIMessageMeta> getConversationHistoryMeta(
        @NotNull String sessionId,
        @NotNull String engineId,
        @NotNull Instant from,
        @NotNull Instant to
    ) throws DBException {
        return List.of();
    }

    @Override
    public boolean canPersist() {
        return false;
    }
}
