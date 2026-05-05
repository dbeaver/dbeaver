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
package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProviderPropertyDescriptor extends PropertyDescriptor {

    private static final Log log = Log.getLog(ProviderPropertyDescriptor.class);
    private static final String ATTR_SUPPORTED_CONFIGURATION_TYPES = "supportedConfigurationTypes";
    private final Set<DBPDriverConfigurationType> configurationTypes;

    public ProviderPropertyDescriptor(String category, IConfigurationElement config) {
        super(category, config);
        var configurationTypes = config.getAttribute(ATTR_SUPPORTED_CONFIGURATION_TYPES);
        if (CommonUtils.isEmpty(configurationTypes)) {
            this.configurationTypes = Set.of(DBPDriverConfigurationType.MANUAL, DBPDriverConfigurationType.URL); // by default
        } else {
            String[] supportedConfigurationTypes = CommonUtils.split(configurationTypes, ",");
            this.configurationTypes = Stream.of(supportedConfigurationTypes)
                .map(DBPDriverConfigurationType::valueOf)
                .collect(Collectors.toSet());
        }
    }

    public static List<ProviderPropertyDescriptor> extractProviderProperties(IConfigurationElement config) {
        String category = getPropertyCategory(config);
        List<ProviderPropertyDescriptor> properties = new ArrayList<>();
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY);
        for (IConfigurationElement prop : propElements) {
            properties.add(new ProviderPropertyDescriptor(category, prop));
        }
        return properties;
    }

    public Set<DBPDriverConfigurationType> getConfigurationTypes() {
        return configurationTypes;
    }
}
