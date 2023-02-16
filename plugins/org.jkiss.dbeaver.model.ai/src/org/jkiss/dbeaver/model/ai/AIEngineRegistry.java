/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

/**
 * AI engine settings
 */
public class AIEngineRegistry {

    private static AIEngineRegistry instance = null;

    public synchronized static AIEngineRegistry getInstance() {
        if (instance == null) {
            //instance = new AIEngineRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

/*
    private final List<EntityEditorDescriptor> entityEditors = new ArrayList<>();
    private final List<EntityConfiguratorDescriptor> entityConfigurators = new ArrayList<>();
    private final Map<String, List<EntityEditorDescriptor>> positionsMap = new HashMap<>();

    public AIEngineRegistry(IExtensionRegistry registry) {
        // Create default editor
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EntityEditorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("completionEngine".equals(ext.getName())) {
                EntityEditorDescriptor descriptor = new EntityEditorDescriptor(ext);
                entityEditors.add(descriptor);
                List<EntityEditorDescriptor> list = positionsMap.computeIfAbsent(
                    descriptor.getPosition(), k -> new ArrayList<>());
                list.add(descriptor);
            } else if (TAG_CONFIGURATOR.equals(ext.getName())) {
                EntityConfiguratorDescriptor descriptor = new EntityConfiguratorDescriptor(ext);
                entityConfigurators.add(descriptor);
            }
        }
    }

*/

}
