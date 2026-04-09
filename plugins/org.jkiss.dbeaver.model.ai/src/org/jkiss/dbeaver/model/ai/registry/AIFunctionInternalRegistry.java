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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI function registry
 */
public class AIFunctionInternalRegistry {

    private final Map<String, AIFunctionDescriptor> functionsById = new LinkedHashMap<>();

    public AIFunctionInternalRegistry(@NotNull AIToolboxInternalDescriptor toolbox) {
        IConfigurationElement[] extElements = Platform.getExtensionRegistry()
            .getConfigurationElementsFor(AIFunctionInternalDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("function".equals(ext.getName())) {
                AIFunctionInternalDescriptor fd = new AIFunctionInternalDescriptor(toolbox, ext);
                functionsById.put(fd.getId(), fd);
            }
        }
    }

    @Nullable
    public AIFunctionDescriptor getFunction(@NotNull String id) {
        return functionsById.get(id);
    }

    @NotNull
    public List<AIFunctionDescriptor> getAllFunctions(@NotNull AIFunctionPurpose purpose) {
        return functionsById.values().stream()
            .filter(f ->
                f.getPurpose() == AIFunctionPurpose.ALL
                    || purpose == AIFunctionPurpose.ALL
                    || f.getPurpose() == purpose)
            .toList();
    }

    @NotNull
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        AIFunction function = descriptor.getInstance();
        return function.callFunction(context, arguments);
    }
}
