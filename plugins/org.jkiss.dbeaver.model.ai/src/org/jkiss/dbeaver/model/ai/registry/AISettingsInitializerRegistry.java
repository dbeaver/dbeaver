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
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.ai.internal.AISettingsInitializer;

public class AISettingsInitializerRegistry {

    private static AISettingsInitializerRegistry instance = null;
    private final AISettingsInitializerDescriptor descriptor;

    public static synchronized AISettingsInitializerRegistry getInstance() {
        if (instance == null) {
            IConfigurationElement[] elements = Platform.getExtensionRegistry()
                .getConfigurationElementsFor(AISettingsInitializerDescriptor.EXTENSION_ID);
            IConfigurationElement initializerWithMaxPriority = findInitializerWithMaxPriority(elements);
            AISettingsInitializerDescriptor initializerDescriptor = new AISettingsInitializerDescriptor(initializerWithMaxPriority);
            instance = new AISettingsInitializerRegistry(initializerDescriptor);
        }

        return instance;
    }

    public AISettingsInitializerRegistry(AISettingsInitializerDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public AISettingsInitializer createInitializer() throws IllegalStateException {
        try {
            return descriptor.createInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Error creating AI settings initializer", e);
        }
    }

    private static IConfigurationElement findInitializerWithMaxPriority(IConfigurationElement[] elements) {
        IConfigurationElement topElement = null;
        int maxPriority = Integer.MIN_VALUE;
        for (IConfigurationElement element : elements) {
            if (!element.getName().equals("initializer")) {
                continue;
            }

            String priorityStr = element.getAttribute("priority");
            int priority = priorityStr != null ? Integer.parseInt(priorityStr) : 0;
            if (topElement == null || priority > maxPriority) {
                topElement = element;
                maxPriority = priority;
            }
        }
        return topElement;
    }
}
