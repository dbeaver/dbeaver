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
package org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy;

import org.jkiss.code.NotNull;

public class ChatMessage {

    @NotNull
    private String role;
    private String content;
    // name is optional, The name of the author of this message.
    // May contain a-z, A-Z, 0-9, and underscores, with a maximum length of 64 characters.
    private String name;

    public ChatMessage(@NotNull String role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatMessage(@NotNull String role, String content, String name) {
        this.role = role;
        this.content = content;
        this.name = name;
    }

    @NotNull
    public String getRole() {
        return role;
    }

    public void setRole(@NotNull String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
