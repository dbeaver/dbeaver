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
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request to AI engine
 */
public final class AIEngineRequest {
    @NotNull
    private final List<AIMessage> messages;
    private final List<AIFunctionDescriptor> functions = new ArrayList<>();

    public AIEngineRequest(
        @NotNull List<AIMessage> messages
    ) {
        this.messages = messages;
    }

    public AIEngineRequest(
        @NotNull AIMessage message
    ) {
        this.messages = List.of(message);
    }

    @NotNull
    public List<AIMessage> getMessages() {
        return messages;
    }

    @NotNull
    public List<AIFunctionDescriptor> getFunctions() {
        return functions;
    }

    public void setFunctions(@NotNull List<AIFunctionDescriptor> functions) {
        this.functions.clear();
        this.functions.addAll(functions);
    }

}
