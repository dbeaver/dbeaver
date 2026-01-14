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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithReturn;

public class AIFunctionResult {
    public enum FunctionType {
        INFORMATION,
        ACTION
    }


    private final AIFunctionResult.FunctionType type;
    private final Object value;
    @Nullable
    private final DBRRunnableWithReturn<?> callback;

    public AIFunctionResult(
        @NotNull AIFunctionResult.FunctionType type,
        @NotNull Object value
    ) {
        this(type, value, null);
    }

    public AIFunctionResult(
        @NotNull AIFunctionResult.FunctionType type,
        @NotNull Object value,
        @Nullable DBRRunnableWithReturn<?> callback
    ) {
        this.type = type;
        this.value = value;
        this.callback = callback;
    }

    @NotNull
    public AIFunctionResult.FunctionType getType() {
        return type;
    }

    @NotNull
    public Object getValue() {
        return value;
    }

    @Nullable
    public DBRRunnableWithReturn<?> getCallback() {
        return callback;
    }
}
