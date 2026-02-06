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
package org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIUsage;

import java.util.List;

/**
 * Object containing a response from the chat completions api.
 */
public class ChatCompletionResult {

    /**
     * Unique id assigned to this chat completion.
     */
    private String id;

    /**
     * The type of object returned, should be "chat.completion"
     */
    private String object;

    /**
     * The creation time in epoch seconds.
     */
    private long created;
    
    /**
     * The GPT model used.
     */
    private String model;

    /**
     * A list of all generated completions.
     */
    private List<ChatCompletionChoice> choices;

    @Nullable
    private OAICompletionUsage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatCompletionChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<ChatCompletionChoice> choices) {
        this.choices = choices;
    }

    @Nullable
    public OAICompletionUsage getUsage() {
        return usage;
    }

    @Nullable
    public AIUsage getAIUsage() {
        if (usage == null) {
            return null;
        }

        int cachedTokens = usage.promptTokensDetails() != null ? usage.promptTokensDetails().cachedTokens() : 0;
        int reasoningTokens = usage.completionTokensDetails() != null ? usage.completionTokensDetails().reasoningTokens() : 0;

        return new AIUsage(
            usage.promptTokens(),
            cachedTokens,
            usage.completionTokens(),
            reasoningTokens
        );
    }
}
