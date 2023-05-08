/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Completion request
 */
public class DAICompletionRequest {

    private String beginText;
    private String promptText;
    private String endText;
    private boolean beginTruncated;
    private boolean endTruncated;

    private DAICompletionScope scope;
    private List<DBSEntity> customEntities;

    private final Map<String, Object> completionOptions = new HashMap<>();

    public String getBeginText() {
        return beginText;
    }

    public void setBeginText(String beginText) {
        this.beginText = beginText;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getEndText() {
        return endText;
    }

    public void setEndText(String endText) {
        this.endText = endText;
    }

    public boolean isBeginTruncated() {
        return beginTruncated;
    }

    public void setBeginTruncated(boolean beginTruncated) {
        this.beginTruncated = beginTruncated;
    }

    public boolean isEndTruncated() {
        return endTruncated;
    }

    public void setEndTruncated(boolean endTruncated) {
        this.endTruncated = endTruncated;
    }

    public DAICompletionScope getScope() {
        return scope;
    }

    public void setScope(DAICompletionScope scope) {
        this.scope = scope;
    }

    public List<DBSEntity> getCustomEntities() {
        return customEntities;
    }

    public void setCustomEntities(List<DBSEntity> customEntities) {
        this.customEntities = customEntities;
    }

    public Map<String, Object> getCompletionOptions() {
        return completionOptions;
    }

    /**
     * Sets completion option value
     */
    public void setCompletionOption(String name, Object value) {
        completionOptions.put(name, value);
    }
}
