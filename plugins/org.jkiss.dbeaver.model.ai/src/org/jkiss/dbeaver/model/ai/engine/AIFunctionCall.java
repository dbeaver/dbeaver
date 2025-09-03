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

import java.util.Map;

/**
 * AI function call info
 */
public class AIFunctionCall {
    @NotNull
    private final String function;
    @NotNull
    private final Map<String, Object> arguments;
    @Nullable
    private String hint;

    public AIFunctionCall(@NotNull String function, @NotNull Map<String, Object> arguments) {
        this.function = function;
        this.arguments = arguments;
    }

    @NotNull
    public String getFunction() {
        return function;
    }

    @NotNull
    public Map<String, Object> getArguments() {
        return arguments;
    }

    @Nullable
    public String getHint() {
        return hint;
    }

    public void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    @Override
    public String toString() {
        return function + "(" + arguments + ")";
    }
}
