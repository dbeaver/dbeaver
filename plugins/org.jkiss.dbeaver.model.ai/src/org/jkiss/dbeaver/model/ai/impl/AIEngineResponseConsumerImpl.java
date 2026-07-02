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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class AIEngineResponseConsumerImpl implements AIEngineResponseConsumer {
    private static final Log log = Log.getLog(AIEngineResponseConsumerImpl.class);

    private boolean logResponses;
    private final AIChatResponseConsumer chatListener;
    private final DBRProgressMonitor monitor;
    private final AIEngine<?> engine;
    private final AIChatConversation conversation;
    private final AIEngineDescriptor engineDescriptor;
    private boolean closed = false;
    private final AtomicReference<AIUsage> usageRef = new AtomicReference<>();
    private final Instant startTime = Instant.now();
    private final AtomicInteger systemPromptLength = new AtomicInteger(0);
    private final List<AIFunctionCall> functionCalls = new ArrayList<>();
    private final Consumer<List<AIFunctionCall>> functionCallConsumer;

    public AIEngineResponseConsumerImpl(
        @NotNull AIChatResponseConsumer chatListener,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngine<?> engine,
        @NotNull AIChatConversation conversation,
        @NotNull AIEngineDescriptor engineDescriptor,
        @NotNull Consumer<List<AIFunctionCall>> fcConsumer
    ) {
        this.chatListener = chatListener;
        this.monitor = monitor;
        this.engine = engine;
        this.conversation = conversation;
        this.engineDescriptor = engineDescriptor;
        this.functionCallConsumer = fcConsumer;
    }

    void setLogResponses(boolean logResponses) {
        this.logResponses = logResponses;
    }

    @Override
    public void nextChunk(@NotNull AIEngineResponseChunk chunk) {
        if (monitor.isCanceled() || !conversation.isActive()) {
            close(true);
            return;
        }
        if (chunk.getFunctionCall() != null) {
            if (logResponses) {
                System.err.println("AI function call " + chunk.getFunctionCall());
            }
            functionCalls.add(chunk.getFunctionCall());
        } else {
            List<String> choices = chunk.getChoices();
            if (!CommonUtils.isEmpty(choices)) {
                if (logResponses) {
                    System.err.print(choices);
                }
                chatListener.nextMessageChunk(choices.getFirst());
            }
        }
    }

    @Override
    public void error(@NotNull Throwable throwable) {
        if (logResponses) {
            throwable.printStackTrace(System.err);
        }
        chatListener.error(throwable);
        close(true);
    }

    @Override
    public void completeBlock() {
        boolean hasFunctions = !functionCalls.isEmpty();

        close(!hasFunctions);

        // Request completed
        // Now let's check function calls
        if (hasFunctions) {
            functionCallConsumer.accept(functionCalls);
        }
    }

    @Override
    public void usage(@Nullable AIUsage usage) {
        usageRef.set(usage);
    }

    @Override
    public void systemPromptLength(int length) {
        systemPromptLength.set(length);
    }

    @Override
    public void warning(@NotNull String message) {
        chatListener.warning(message);
    }

    private void close(boolean finishConversation) {
        if (closed) {
            return;
        }

        AIUsage usage = usageRef.get() != null ? usageRef.get() : new AIUsage(0, 0, 0, 0);
        AIMessageMeta messageMeta = new AIMessageMeta(
            AIMetaTypes.PROMPT,
            engineDescriptor.getId(),
            engine.getProperties().getModel(),
            usage,
            Duration.between(startTime, Instant.now()),
            systemPromptLength.get()
        );

        chatListener.complete(List.of(messageMeta), finishConversation);

        try {
            engine.close();
        } catch (Exception e) {
            log.error(e);
        }
        closed = true;
    }

}
