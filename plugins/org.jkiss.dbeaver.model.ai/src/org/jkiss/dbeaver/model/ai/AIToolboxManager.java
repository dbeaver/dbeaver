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
import org.jkiss.dbeaver.model.ai.registry.AIFunctionCategoryDescriptor;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.util.List;
import java.util.Map;

/**
 * AI toolbox manager.
 */
public interface AIToolboxManager {

    @Nullable
    AIToolbox getToolbox(@NotNull String id);

    @NotNull
    List<AIToolbox> getAllToolboxes();

    @NotNull
    List<AIFunctionDescriptor> getAllFunctions(@NotNull AIFunctionPurpose purpose);

    @Nullable
    AIFunctionDescriptor getFunctionByFullId(@NotNull String id);

    @NotNull
    List<AIFunctionCategoryDescriptor> getAllCategories();

    @NotNull
    AIFunctionSettings getFunctionSettings();

    void saveFunctionSettings() throws DBCException;

    /**
     * Saves external toolbox configurations.
     *
     * @param toolboxConfigurations map of toolbox ID to its configuration properties
     */
    void saveExternalToolboxes(@NotNull Map<String, Map<String, Object>> toolboxConfigurations) throws DBException;
}
