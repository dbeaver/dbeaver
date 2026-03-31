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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * AI Assistant interface.
 * Provides various methods for AI-based operations.
 */
public interface AIAssistant {

    /**
     * Generates text according to the prompt
     *
     * @param functionContext database context. Creates database snapshot according to this context.
     * @param messages        user messages
     * @return generated text
     */
    @NotNull
    AIAssistantResponse generateText(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIFunctionContext functionContext,
        @NotNull List<AIMessage> messages
    ) throws DBException;

    boolean isFunctionSupported();

    /**
     * Toolbox manager
     */
    @NotNull
    AIToolboxManager getToolboxManager();
}
