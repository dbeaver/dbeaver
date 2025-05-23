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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;

public class AIAssistantRegistry {

    private static AIAssistantRegistry instance = null;

    private final AIAssistantDescriptor customDescriptor;
    private AIAssistant globalAssistant;

    public synchronized static AIAssistantRegistry getInstance() {
        if (instance == null) {
            instance = new AIAssistantRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    public AIAssistantRegistry(IExtensionRegistry registry) {
        AIAssistantDescriptor customAssistantDescriptor = null;
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(AIAssistantDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("assistant".equals(ext.getName())) {
                customAssistantDescriptor = new AIAssistantDescriptor(ext);
                break;
            }
        }
        this.customDescriptor = customAssistantDescriptor;
    }

    public <T extends AIAssistant> T getAssistant() {
        if (globalAssistant == null) {
            synchronized (this) {
                if (globalAssistant == null) {
                    if (customDescriptor != null) {
                        try {
                            globalAssistant = customDescriptor.createInstance();
                        } catch (DBException e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        globalAssistant = new AIAssistantImpl();
                    }
                }
            }
        }
        return (T)globalAssistant;
    }
}
