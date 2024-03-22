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
package org.jkiss.dbeaver.registry.settings;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyGroupDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductSettingsRegistry {
    public static final String SETTINGS_EXTENSION_ID = "org.jkiss.dbeaver.settings"; //$NON-NLS-1$
    private static ProductSettingsRegistry instance = null;

    private final Map<String, PropertyGroupDescriptor<ProductSettingDescriptor>> settings = new LinkedHashMap<>();

    public synchronized static ProductSettingsRegistry getInstance() {
        if (instance == null) {
            instance = new ProductSettingsRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private ProductSettingsRegistry() {
    }

    private synchronized void loadExtensions(IExtensionRegistry registry) {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SETTINGS_EXTENSION_ID);

        for (IConfigurationElement ext : extConfigs) {
            // Load webServices
            if (PropertyDescriptor.TAG_PROPERTY_GROUP.equals(ext.getName())) {
                parseSettingsGroup(ext, null);
            }
        }
    }

    private void parseSettingsGroup(
        IConfigurationElement ext,
        @Nullable PropertyGroupDescriptor<ProductSettingDescriptor> parentGroup
    ) {
        PropertyGroupDescriptor<ProductSettingDescriptor> settingsGroup = new PropertyGroupDescriptor<>(ext);
        if (settings.containsKey(settingsGroup.getId())) {
            settingsGroup = settings.get(settingsGroup.getId());
        } else {
            settings.put(settingsGroup.getId(), settingsGroup);
        }
        if (parentGroup != null) {
            settingsGroup.setParentGroup(parentGroup);
            parentGroup.addSubGroup(settingsGroup);
        }
        for (IConfigurationElement childExt : ext.getChildren()) {
            if (PropertyDescriptor.TAG_PROPERTY_GROUP.equals(childExt.getName())) {
                parseSettingsGroup(childExt, settingsGroup);
            } else if (PropertyDescriptor.TAG_PROPERTY.equals(childExt.getName())) {
                ProductSettingDescriptor productSettingDescriptor = new ProductSettingDescriptor(settingsGroup.getFullId(),
                    childExt);
                settingsGroup.addProperty(productSettingDescriptor);
            }
        }

    }

    public List<PropertyGroupDescriptor<ProductSettingDescriptor>> getSettings() {
        return new ArrayList<>(settings.values());
    }
}
