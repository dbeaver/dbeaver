/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;

import java.util.Set;

public class ChatFunctionProperty {
    @NotNull
    private transient String name;
    @NotNull
    private String type;
    private transient Boolean required;
    private String description;
    private ChatFunctionProperty items;
    @SerializedName("enum")
    private Set<?> enumValues;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChatFunctionProperty getItems() {
        return items;
    }

    public void setItems(ChatFunctionProperty items) {
        this.items = items;
    }

    public Set<?> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Set<?> enumValues) {
        this.enumValues = enumValues;
    }
}