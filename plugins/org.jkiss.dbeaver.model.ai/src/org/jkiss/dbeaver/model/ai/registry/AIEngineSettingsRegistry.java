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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineSettingsSerDe;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class AIEngineSettingsRegistry {

    private static AIEngineSettingsRegistry INSTANCE;
    private final List<AIEngineSettingsSerDe<?>> serDes = new ArrayList<>();
    private final Map<String, String> replacements = new HashMap<>();

    public static synchronized AIEngineSettingsRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AIEngineSettingsRegistry();
            INSTANCE.loadExtensions(Platform.getExtensionRegistry());
        }
        return INSTANCE;

    }

    @NotNull
    public List<AIEngineSettingsSerDe<?>> getSerDes() {
        return serDes;
    }

    @NotNull
    public Map<String, String> getReplacements() {
        return Collections.unmodifiableMap(replacements);
    }

    private void loadExtensions(IExtensionRegistry registry) {
        IConfigurationElement[] serDeElements = registry.getConfigurationElementsFor(AIEngineConfigurationSerDeDescriptor.EXTENSION_ID);
        for (IConfigurationElement cfg : serDeElements) {
            String id = cfg.getAttribute("id");
            String replaces = cfg.getAttribute("replaces");
            if (!CommonUtils.isEmpty(replaces)) {
                replacements.put(replaces, id);
            }
        }
        for (IConfigurationElement iConfigurationElement : serDeElements) {
            AIEngineConfigurationSerDeDescriptor descriptor = new AIEngineConfigurationSerDeDescriptor(iConfigurationElement);
            if (replacements.containsKey(descriptor.getId())) {
                // Skip
                continue;
            }
            try {
                serDes.add(descriptor.createInstance());
            } catch (DBException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
