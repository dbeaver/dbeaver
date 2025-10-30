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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithReturn;

import java.util.List;

public sealed interface MessageChunk {
    @NotNull
    String toRawString();

    @Nullable
    default DBRRunnableWithReturn<?> getCallback() {
        return null;
    }

    record Text(@NotNull String text, @NotNull List<LinkPosition> links) implements MessageChunk {
        @NotNull
        @Override
        public String toRawString() {
            return text;
        }

        @NotNull
        public List<LinkPosition> getLinks() {
            return links;
        }
    }

    record Code(@NotNull String text, @NotNull String language) implements MessageChunk {
        @NotNull
        @Override
        public String toRawString() {
            return "```" + language + "\n" + text + "\n```";
        }
    }

    record Link(@NotNull String text, @Nullable DBRRunnableWithReturn<?> callback) implements MessageChunk {
        @Nullable
        @Override
        public DBRRunnableWithReturn<?> getCallback() {
            return callback;
        }

        @NotNull
        @Override
        public String toRawString() {
            return text;
        }
    }
}
