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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
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
    private final ObjectType propertiesType;
    private final LazyValue<AIEngine, DBException> factoryInstance;

    protected AIEngineDescriptor(@NotNull IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.contributorConfig = contributorConfig;
        this.objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        this.propertiesType = new ObjectType(contributorConfig, "properties");
        this.factoryInstance = new LazyValue<>() {
            @NotNull
            @Override
            protected AIEngine initialize() throws DBException {
                return objectType.createInstance(AIEngine.class);
            }
        };

        for (IConfigurationElement propGroup : ArrayUtils.safeArray(contributorConfig.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            properties.addAll(PropertyDescriptor.extractProperties(propGroup));
        }
    }

    @NotNull
    public String getId() {
        return contributorConfig.getAttribute("id");
    }

    @NotNull
    public String getLabel() {
        return contributorConfig.getAttribute("label");
    }

    @Nullable
    public String getReplaces() {
        return contributorConfig.getAttribute("replaces");
    }

    public boolean isDefault() {
        return CommonUtils.toBoolean(contributorConfig.getAttribute("default"));
    }

    @NotNull
    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    @NotNull
    public Class<? extends AIEngineProperties> getPropertiesType() {
        Class<? extends AIEngineProperties> propsClass = propertiesType.getObjectClass(AIEngineProperties.class);
        if (propsClass == null) {
            throw new IllegalStateException("AI properties class not specified (" + getId() + ")");
        }
        return propsClass;
    }


    @NotNull
    public <T extends AIEngineProperties> T createPropertiesInstance() throws DBException {
        return (T)propertiesType.createInstance(AIEngineProperties.class);
    }

    @NotNull
    public AIEngine createInstance() throws DBException {
        return factoryInstance.getInstance();
    }
}
