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
package org.jkiss.dbeaver.model.ai.completion;

/**
 * Role of the message
 */
public enum DAIChatRole {
    // System messages like context description
    SYSTEM(false),
    // User prompts
    USER(false),
    // Response from AI
    ASSISTANT(false),
    // Error messages
    ERROR(true);

    private final boolean isLocal;

    DAIChatRole(boolean isLocal) {
        this.isLocal = isLocal;
    }

    /**
     * Local messages are never sent to AI engine, they exist only on dbeaver side.
     */
    public boolean isLocal() {
        return isLocal;
    }

}
