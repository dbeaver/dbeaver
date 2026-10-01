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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIChatMessage;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

final class AIMessageChunkBuffer {
    private static final int FLUSH_DELAY_MS = 100;

    private final Display display;
    private final Object lock = new Object();
    private final BiConsumer<AIChatMessage, String> chunkConsumer;
    private final BooleanSupplier disposedSupplier;
    private final StringBuilder pendingText = new StringBuilder();

    private AIChatMessage pendingMessage;
    private boolean flushScheduled;

    AIMessageChunkBuffer(
        @NotNull Display display,
        @NotNull BiConsumer<AIChatMessage, String> chunkConsumer,
        @NotNull BooleanSupplier disposedSupplier
    ) {
        this.display = display;
        this.chunkConsumer = chunkConsumer;
        this.disposedSupplier = disposedSupplier;
    }

    void append(@NotNull AIChatMessage message, @NotNull String chunk) {
        if (disposedSupplier.getAsBoolean()) {
            return;
        }

        if (isNextMessage(message)) {
            flushNow();
        }

        synchronized (lock) {
            pendingMessage = message;
            pendingText.append(chunk);
        }
        scheduleFlush();
    }

    private boolean isNextMessage(@NotNull AIChatMessage message) {
        synchronized (lock) {
            return pendingMessage != null && pendingMessage != message;
        }
    }

    void flushNow() {
        display.syncExec(this::flushPending);
    }

    void clear() {
        synchronized (lock) {
            pendingMessage = null;
            pendingText.setLength(0);
            flushScheduled = false;
        }
    }

    private void scheduleFlush() {
        synchronized (lock) {
            if (flushScheduled) {
                return;
            }
            flushScheduled = true;
        }
        display.asyncExec(() -> display.timerExec(FLUSH_DELAY_MS, this::flushPending));
    }

    private void flushPending() {
        if (disposedSupplier.getAsBoolean()) {
            clear();
            return;
        }

        AIChatMessage message;
        String text;
        synchronized (lock) {
            if (pendingMessage == null || pendingText.isEmpty()) {
                flushScheduled = false;
                return;
            }

            message = pendingMessage;
            text = pendingText.toString();
            pendingMessage = null;
            pendingText.setLength(0);
            flushScheduled = false;
        }
        chunkConsumer.accept(message, text);
    }
}
