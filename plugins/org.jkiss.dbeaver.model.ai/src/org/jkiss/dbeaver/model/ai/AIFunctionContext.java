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
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * AI function call context
 */
public class AIFunctionContext {

    @NotNull
    private final DBRProgressMonitor monitor;
    @Nullable
    private final AIDatabaseContext context;
    @NotNull
    private final AIFunction systemGenerator;
    @NotNull
    private final List<AIMessage> promptMessages;

    public AIFunctionContext(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIFunction systemGenerator,
        @NotNull List<AIMessage> promptMessages
    ) {
        this.monitor = monitor;
        this.context = context;
        this.systemGenerator = systemGenerator;
        this.promptMessages = promptMessages;
    }

    @NotNull
    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    @Nullable
    public AIDatabaseContext getContext() {
        return context;
    }

    @NotNull
    public AIFunction getSystemGenerator() {
        return systemGenerator;
    }

    @NotNull
    public List<AIMessage> getPromptMessages() {
        return promptMessages;
    }
}
