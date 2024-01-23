/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.completion;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.code.NotNull;

/**
 * Completion session
 */
public class DAICompletionSession {
    private final List<DAICompletionMessage> allMessages;
    private final List<DAICompletionMessage> userMessages;

    public DAICompletionSession() {
        this.allMessages = new ArrayList<>();
        this.userMessages = new ArrayList<>();
    }

    public void add(@NotNull DAICompletionMessage message) {
        if (message.role() == DAICompletionMessage.Role.USER) {
            userMessages.add(message);
        }
        allMessages.add(message);
    }

    public void remove(@NotNull DAICompletionMessage message) {
        if (message.role() == DAICompletionMessage.Role.USER) {
            userMessages.remove(message);
        }
        allMessages.remove(message);
    }

    public void clear() {
        allMessages.clear();
        userMessages.clear();
    }

    @Deprecated(forRemoval = true)
    public List<DAICompletionMessage> getAllMessages() {
        return getMessages(true);
    }

    @NotNull
    public List<DAICompletionMessage> getMessages(boolean sendAllMessages) {
        if (sendAllMessages) {
            return allMessages;
        }
        return userMessages;
    }
}
