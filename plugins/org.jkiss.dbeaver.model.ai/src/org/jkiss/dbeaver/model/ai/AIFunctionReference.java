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
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;

/**
 * Reference to AI function.
 *
 * Can be returned in AI responses, signaling that certain AI function was used.
 * This information can be used in UI to render links which trigger some UI actions.
 */
public class AIFunctionReference {

    @NotNull
    private final AIFunctionDescriptor function;
    @Nullable
    private final String text;

    public AIFunctionReference(@NotNull AIFunctionDescriptor function, @Nullable String text) {
        this.function = function;
        this.text = text;
    }

    @NotNull
    public AIFunctionDescriptor getFunction() {
        return function;
    }

    @Nullable
    public String getText() {
        return text;
    }
}
