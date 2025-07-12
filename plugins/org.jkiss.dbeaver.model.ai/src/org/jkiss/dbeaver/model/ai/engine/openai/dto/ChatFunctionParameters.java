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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatFunctionParameters {

    private final String type = "object";

    private final HashMap<String, ChatFunctionProperty> properties = new HashMap<>();

    private List<String> required;

    public String getType() {
        return type;
    }

    public HashMap<String, ChatFunctionProperty> getProperties() {
        return properties;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public void addProperty(ChatFunctionProperty property) {
        properties.put(property.getName(), property);
        if (Boolean.TRUE.equals(property.getRequired())) {
            if (this.required == null) {
                this.required = new ArrayList<>();
            }
            this.required.add(property.getName());
        }
    }
}
