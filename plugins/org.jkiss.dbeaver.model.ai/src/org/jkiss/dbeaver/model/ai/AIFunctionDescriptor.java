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
import org.jkiss.dbeaver.model.DBPImage;

/**
 * AI function metadata.
 */
public interface AIFunctionDescriptor {

    @NotNull
    AIToolbox getToolbox();

    @NotNull
    String getId();

    @NotNull
    default String getFullId() {
        return getFullFunctionId(getToolbox().getToolboxId(), getId());
    }

    @NotNull
    String getName();
    
    @Nullable
    DBPImage getIcon();

    @NotNull
    AIFunctionType getType();

    @NotNull
    AIFunctionPurpose getPurpose();

    @Nullable
    String getCategoryId();

    @Nullable
    String getAiDescription();

    /**
     * Function which returns information about surrounding UI (e.g. open windows, active editor, etc)
     */
    boolean isUI();

    /**
     * Indicates whether the function is a system function.
     * System functions are not shown in the UI and can be executed without confirmation.
     */
    boolean isSystem();

    boolean isEnabledByDefault();

    @NotNull
    AIFunctionAllowMode getDefaultAllowMode();

    @NotNull
    AIFunctionParameter[] getParameters();

    @Nullable
    AIFunctionParameter getParameter(@NotNull String name);

    @NotNull
    String[] getDependsOn();

    @NotNull
    default AIFunctionVerifier.FunctionState getFunctionState(@NotNull AIFunctionContext functionContext) {
        return getInstance() instanceof AIFunctionVerifier verifier ?
            verifier.getFunctionState(functionContext, this) :
            AIFunctionVerifier.FunctionState.APPLICABLE;
    }

    @NotNull
    AIFunction getInstance();

    @NotNull
    static String getFullFunctionId(@NotNull String toolboxId, @NotNull String toolId) {
        return toolboxId + "_" + toolId;
    }
}
