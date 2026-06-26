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

import java.util.List;

/**
 * Chat content consumer.
 * Content originally come from AI engines thru AIEngineResponseConsumer.
 * Then transformed and passed to this interface. Which usually holds reference to AIChatSession
 * which updates conversations and triggers UI listeners.
 */
public interface AIChatResponseConsumer {

    void nextMessageChunk(@NotNull String delta);

    void processFunctionCall(@NotNull AIMessage fcMessage);

    void warning(@NotNull String message);

    void error(@NotNull Throwable throwable);

    void complete(
        @NotNull List<AIMessageMeta> meta,
        boolean finishConversation
    );
}
