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

// Java
package org.jkiss.dbeaver.model.ai.qm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * AI Chat storage
 */
public interface AIChatStorage {

    @NotNull
    List<QMAIConversationHistory> findConversations(@NotNull String sessionId) throws DBException;

    void saveConversation(
        @NotNull String sessionId,
        @NotNull QMAIConversationHistory chat
    ) throws DBException;

    void appendMessages(
        @NotNull String conversationId,
        @NotNull List<QMAIChatMessage> messages
    ) throws DBException;

    void deleteMessage(
        @NotNull String conversationId,
        int messageId
    ) throws DBException;

    void extendContext(
        @NotNull String conversationId,
        @NotNull Set<QMAIContextObject> extra
    ) throws DBException;

    void deleteConversation(
        @NotNull String conversationId
    ) throws DBException;

    void renameConversation(
        @NotNull String conversationId,
        @NotNull String newName
    ) throws DBException;

    @NotNull
    List<QMAIMessageMeta> getConversationHistoryMeta(
        @NotNull UUID conversationId
    ) throws DBException;

    @NotNull
    List<QMAIMessageMeta> getConversationHistoryMeta(
        @NotNull String sessionId,
        @NotNull String engineId,
        @NotNull Instant from,
        @NotNull Instant to
    ) throws DBException;

    default boolean canPersist() {
        return true;
    }
}
