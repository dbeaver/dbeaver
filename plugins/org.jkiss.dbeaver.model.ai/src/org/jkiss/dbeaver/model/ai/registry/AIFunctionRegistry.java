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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;

import java.util.*;

/**
 * AI function registry
 */
public class AIFunctionRegistry {

    private static final Log log = Log.getLog(AIFunctionRegistry.class);
    private static AIFunctionRegistry instance;

    public static synchronized AIFunctionRegistry getInstance() {
        if (instance == null) {
            instance = new AIFunctionRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, AIFunctionDescriptor> functionsById = new LinkedHashMap<>();
    private final Map<String, AIFunctionCategoryDescriptor> categoriesById = new LinkedHashMap<>();

    public AIFunctionRegistry(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(AIFunctionDescriptor.EXTENSION_ID);
        for (IConfigurationElement el : extElements) {
            if ("category".equals(el.getName())) {
                var cd = new AIFunctionCategoryDescriptor(el);
                categoriesById.put(cd.getId(), cd);
            }
        }
        for (IConfigurationElement ext : extElements) {
            if ("function".equals(ext.getName())) {
                AIFunctionDescriptor fd = new AIFunctionDescriptor(ext);
                functionsById.put(fd.getId(), fd);
            }
        }
    }

    @Nullable
    public AIFunctionDescriptor getFunction(@NotNull String id) {
        return functionsById.get(id);
    }

    @NotNull
    public List<AIFunctionDescriptor> getAllFunctions() {
        return new ArrayList<>(functionsById.values());
    }

    @NotNull
    public List<AIFunctionCategoryDescriptor> getAllCategories() {
        return new ArrayList<>(categoriesById.values());
    }

    @NotNull
    public Map<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> getFunctionsByCategory() {
        Map<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> map = new LinkedHashMap<>();
        for (var cat : categoriesById.values()) {
            map.put(cat, new ArrayList<>());
        }
        for (AIFunctionDescriptor f : functionsById.values()) {
            var cat = categoriesById.get(f.getCategoryId());
            if (cat != null) {
                map.get(cat).add(f);
            }
        }
        return map;
    }

    @NotNull
    public Set<String> getDefaultEnabledCategoryIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (var c : categoriesById.values()) {
            if (c.isEnabledByDefault()) {
                ids.add(c.getId());
            }
        }
        return ids;
    }

    @NotNull
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        AIFunction function = descriptor.createInstance();
        return function.callFunction(context, arguments);
    }
}
