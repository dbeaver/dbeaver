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
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class AIEngineDescriptor extends AbstractDescriptor {

    private final IConfigurationElement contributorConfig;
    private final List<DBPPropertyDescriptor> properties = new ArrayList<>();

    protected AIEngineDescriptor(IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.contributorConfig = contributorConfig;
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

    String getReplaces() {
        return contributorConfig.getAttribute("replaces");
    }

    public boolean isDefault() {
        return CommonUtils.toBoolean(contributorConfig.getAttribute("default"));
    }

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public DAICompletionEngine createInstance() throws DBException {
        ObjectType objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        return objectType.createInstance(DAICompletionEngine.class);
    }

}
