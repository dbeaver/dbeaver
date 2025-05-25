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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * Descriptor for AI engine configuration serializer/deserializer
 */
public class AIEngineConfigurationSerDeDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "com.dbeaver.ai.engine.settingsSerDe";

    private final IConfigurationElement contributorConfig;

    public AIEngineConfigurationSerDeDescriptor(IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.contributorConfig = contributorConfig;
    }

    public String getId() {
        return contributorConfig.getAttribute("id");
    }

    /**
     * Creates an instance of the AI engine configuration serializer/deserializer.
     */
    public AIEngineSettingsSerDe<?> createInstance() throws DBException {
        ObjectType objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        return objectType.createInstance(AIEngineSettingsSerDe.class);
    }
}
