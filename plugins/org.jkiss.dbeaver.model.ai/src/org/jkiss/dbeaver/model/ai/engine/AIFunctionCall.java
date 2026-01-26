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

package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;

import java.util.Map;

/**
 * AI function call info
 */
public class AIFunctionCall {
    @Nullable
    private String functionName;
    @Nullable
    private Map<String, Object> arguments;
    @Nullable
    private String hint;
    @Nullable
    private AIFunctionDescriptor function;

    public AIFunctionCall() {
    }

    public AIFunctionCall(@NotNull String functionName, @Nullable Map<String, Object> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Nullable
    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(@NotNull String functionName) {
        this.functionName = functionName;
    }

    @Nullable
    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(@NotNull Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    @Nullable
    public String getHint() {
        return hint;
    }

    public void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    @Nullable
    public AIFunctionDescriptor getFunction() {
        return function;
    }

     public void setFunction(@NotNull AIFunctionDescriptor function) {
        this.function = function;
    }

    @Override
    public String toString() {
        return functionName + "(" + arguments + ")";
    }

}
