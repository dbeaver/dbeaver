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

public interface AIChatListener {

    default void messageAdded(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
        // do nothing by default
    }

    default void messageRemoved(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
        // do nothing by default
    }

    default void conversationChanged(@NotNull AIChatConversation conversation) {
        // do nothing by default
    }

    default void settingsChanged(@Nullable AIContextSettings settings) {
        // do nothing by default
    }

    default void conversationRenamed(@NotNull AIChatConversation conversation, @NotNull String newName) {
        // do nothing by default
    }

    /**
     * Called when AI request processing starts/ends
     */
    default void busyChanged(boolean busy) {
        // do nothing by default
    }

    /**
     * Called when a new streaming message chunk is received
     */
    default void messageChunkReceived(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message, @NotNull String chunk) {
        // do nothing by default
    }
}
