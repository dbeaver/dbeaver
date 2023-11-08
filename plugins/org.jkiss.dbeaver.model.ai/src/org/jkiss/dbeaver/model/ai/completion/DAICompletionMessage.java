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

import org.jkiss.code.NotNull;

/**
 * Represents a single completion message
 *
 * @param role    role of the message
 * @param content content of the message
 */
public record DAICompletionMessage(@NotNull Role role, @NotNull String content) {
    /**
     * Role of the message
     */
    public enum Role {
        SYSTEM,
        USER;

        @NotNull
        public String getId() {
            return switch (this) {
                case SYSTEM -> "system";
                case USER -> "user";
            };
        }
    }
}
