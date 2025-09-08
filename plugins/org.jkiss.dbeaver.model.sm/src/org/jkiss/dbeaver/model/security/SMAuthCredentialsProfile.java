/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.auth.AuthPropertyDescriptor;
import org.jkiss.dbeaver.model.impl.LocalizedPropertyDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Credentials profile.
 * Set of user credentials required by auth provider.
 */
public class SMAuthCredentialsProfile {
    private final String id;
    private final String label;
    private final String description;
    private final Map<String, AuthPropertyDescriptor> credentialParameters = new LinkedHashMap<>();

    public SMAuthCredentialsProfile(IConfigurationElement cfg) {
        this.id = cfg.getAttribute("id");
        this.label = cfg.getAttribute("label");
        this.description = cfg.getAttribute("description");
        for (IConfigurationElement propGroup : ArrayUtils.safeArray(cfg.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            String category = propGroup.getAttribute(PropertyDescriptor.ATTR_LABEL);
            IConfigurationElement[] propElements = propGroup.getChildren(PropertyDescriptor.TAG_PROPERTY);
            for (IConfigurationElement prop : propElements) {
                AuthPropertyDescriptor propertyDescriptor = new AuthPropertyDescriptor(category, prop);
                credentialParameters.put(CommonUtils.toString(propertyDescriptor.getId()), propertyDescriptor);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public List<AuthPropertyDescriptor> getCredentialParameters() {
        return new ArrayList<>(credentialParameters.values());
    }

    public AuthPropertyDescriptor getCredentialParameter(String id) {
        return credentialParameters.get(id);
    }
}
