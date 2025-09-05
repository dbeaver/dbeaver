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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collections;
import java.util.List;

// FIXME: create several subclasses for different types of chunks
public final class AIEngineResponseChunk {
    @NotNull
    private final List<String> choices;
    @Nullable
    private final AIFunctionCall functionCall;

    public AIEngineResponseChunk(
        @NotNull List<String> choices
    ) {
        this.choices = choices;
        this.functionCall = null;
    }

    public AIEngineResponseChunk(@NotNull AIFunctionCall functionCall) {
        this.choices = Collections.emptyList();
        this.functionCall = functionCall;
    }

    @NotNull
    public List<String> getChoices() {
        return choices;
    }

    @Nullable
    public AIFunctionCall getFunctionCall() {
        return functionCall;
    }

    @Override
    public String toString() {
        if (functionCall != null) {
            return functionCall.toString();
        }
        return choices.toString();
    }

}
