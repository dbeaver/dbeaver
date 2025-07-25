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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * AI Assistant interface. Provides methods for AI-based operations.
 */
public interface AIAssistant {

    /**
     * Initializes assistant
     */
    void initialize(@NotNull DBPWorkspace workspace);

    /**
     * Translates text to SQL.
     */
    @NotNull
    String translateTextToSql(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AITranslateRequest request
    ) throws DBException;

    /**
     * Translates a user command to SQL. The active completion engine is used.
     */
    @NotNull
    AICommandResult command(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICommandRequest request
    ) throws DBException;

    /**
     * Returns whether the AI assistant has a valid configuration.
     */
    boolean hasValidConfiguration() throws DBException;

    @NotNull
    AIEngine getActiveEngine() throws DBException;

    @Nullable
    AIEngineDescriptor getActiveEngineDescriptor();
}
