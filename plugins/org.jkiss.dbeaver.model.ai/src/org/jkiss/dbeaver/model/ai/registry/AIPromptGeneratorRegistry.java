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
import org.jkiss.dbeaver.Log;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI engine settings
 */
public class AIPromptGeneratorRegistry {

    private static final Log log = Log.getLog(AIPromptGeneratorRegistry.class);

    private static AIPromptGeneratorRegistry instance = null;

    public static synchronized AIPromptGeneratorRegistry getInstance() {
        if (instance == null) {
            instance = new AIPromptGeneratorRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, AIPromptGeneratorDescriptor> descriptorMap = new LinkedHashMap<>();

    public AIPromptGeneratorRegistry(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(AIPromptGeneratorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("prompt".equals(ext.getName())) {
                AIPromptGeneratorDescriptor descriptor = new AIPromptGeneratorDescriptor(ext);
                descriptorMap.put(descriptor.getId(), descriptor);
            }
        }
    }

    @Nullable
    public AIPromptGeneratorDescriptor getPromptGenerator(@NotNull String id) {
        return descriptorMap.get(id);
    }

    @NotNull
    public List<AIPromptGeneratorDescriptor> getAllPromptGenerator() {
        return descriptorMap.values().stream().toList();
    }

}
