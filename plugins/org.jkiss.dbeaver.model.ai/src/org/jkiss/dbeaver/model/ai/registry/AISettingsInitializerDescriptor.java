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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.internal.AISettingsInitializer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

public class AISettingsInitializerDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "com.dbeaver.ai.settings.initializer";
    private final ObjectType objectType;
    private final int priority;

    protected AISettingsInitializerDescriptor(IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        this.priority = Integer.parseInt(contributorConfig.getAttribute("priority"));
    }

    public ObjectType objectType() {
        return objectType;
    }

    public int getPriority() {
        return priority;
    }

    public AISettingsInitializer createInstance() throws DBException {
        return objectType.createInstance(AISettingsInitializer.class);
    }
}
