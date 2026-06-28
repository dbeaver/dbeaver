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

import java.util.List;

public final class QMAIConversationHistory {
    @NotNull
    private final String id;
    @NotNull
    private String caption;
    @Nullable
    private String promptGeneratorId;
    @Nullable
    private final QMAIDataSource dataSource;
    @NotNull
    private List<QMAIChatMessage> messages;
    @NotNull
    private QMAIContext context;
    private final int nextMessageId;
    private final boolean deleted;

    public QMAIConversationHistory(
        @NotNull String id,
        @NotNull String caption,
        @Nullable String generatorId,
        @Nullable QMAIDataSource dataSource,
        @NotNull List<QMAIChatMessage> messages,
        @NotNull QMAIContext context,
        int nextMessageId,
        boolean deleted
    ) {
        this.id = id;
        this.caption = caption;
        this.promptGeneratorId = generatorId;
        this.dataSource = dataSource;
        this.messages = messages;
        this.context = context;
        this.nextMessageId = nextMessageId;
        this.deleted = deleted;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getCaption() {
        return caption;
    }

    public void setCaption(@NotNull String caption) {
        this.caption = caption;
    }

    @Nullable
    public String getPromptGeneratorId() {
        return promptGeneratorId;
    }

    public void setPromptGeneratorId(@NotNull String promptGeneratorId) {
        this.promptGeneratorId = promptGeneratorId;
    }

    @Nullable
    public QMAIDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public List<QMAIChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(@NotNull List<QMAIChatMessage> messages) {
        this.messages = messages;
    }

    @NotNull
    public QMAIContext getContext() {
        return context;
    }

    public void setContext(@NotNull QMAIContext context) {
        this.context = context;
    }

    public int getNextMessageId() {
        return nextMessageId;
    }

    public boolean isDeleted() {
        return deleted;
    }

}
