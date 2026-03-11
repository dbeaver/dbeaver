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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.util.List;
import java.util.Map;

/**
 * AI toolbox. It is a provider of AI tools (functions).
 * It may be an internal toolbox or an external MCP server.
 */
public interface AIToolbox {

    @NotNull
    String getToolboxId();

    @NotNull
    String getDisplayName();

    @Nullable
    String getDescription();

    boolean isEnabled();

    boolean isAccessible();

    @NotNull
    List<AIFunctionDescriptor> getSupportedFunctions();

    @Nullable
    AIFunctionDescriptor getFunctionById(@NotNull String id);

    @NotNull
    AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException;

}
