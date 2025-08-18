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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineFactory;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LazyValue;

import java.util.ArrayList;
import java.util.List;

public class AIEngineDescriptor extends AbstractDescriptor {

    private final IConfigurationElement contributorConfig;
    private final List<DBPPropertyDescriptor> properties = new ArrayList<>();
    private final ObjectType objectType;
    private final LazyValue<AIEngineFactory<?>, DBException> factoryInstance;

    protected AIEngineDescriptor(IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.contributorConfig = contributorConfig;
        this.objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        this.factoryInstance = new LazyValue<>() {
            @NotNull
            @Override
            protected AIEngineFactory<?> initialize() throws DBException {
                return objectType.createInstance(AIEngineFactory.class, AISettingsRegistry.getInstance());
            }
        };

        for (IConfigurationElement propGroup : ArrayUtils.safeArray(contributorConfig.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            properties.addAll(PropertyDescriptor.extractProperties(propGroup));
        }
    }

    public String getId() {
        return contributorConfig.getAttribute("id");
    }

    public String getLabel() {
        return contributorConfig.getAttribute("label");
    }

    public String getReplaces() {
        return contributorConfig.getAttribute("replaces");
    }

    public boolean isDefault() {
        return CommonUtils.toBoolean(contributorConfig.getAttribute("default"));
    }

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public AIEngine createInstance() throws DBException {
        return factoryInstance.getInstance().getEngine();
    }
}
