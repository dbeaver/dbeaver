/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.easyconfig;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EasyConfigFeatureRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.easyConfig";

    private static EasyConfigFeatureRegistry instance;

    private final List<EasyConfigFeatureDescriptor> features;

    private EasyConfigFeatureRegistry(@NotNull IExtensionRegistry registry) {
        var features = new ArrayList<EasyConfigFeatureDescriptor>();

        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if ("feature".equals(element.getName())) {
                features.add(new EasyConfigFeatureDescriptor(element));
            } else {
                throw new IllegalStateException("Unknown element " + element.getName());
            }
        }

        this.features = features.stream()
            .sorted(Comparator.comparing(EasyConfigFeatureDescriptor::getLabel))
            .toList();
    }

    @NotNull
    public static synchronized EasyConfigFeatureRegistry getInstance() {
        if (instance == null) {
            instance = new EasyConfigFeatureRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<EasyConfigFeatureDescriptor> getFeatures() {
        return features;
    }

    public boolean isFeatureEnabled(@NotNull EasyConfigFeatureDescriptor descriptor) {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        var value = store.getString(getPreferenceKey(descriptor));
        return CommonUtils.toBoolean(value, descriptor.isEnabledByDefault());
    }

    public void setFeatureEnabled(@NotNull EasyConfigFeatureDescriptor descriptor, boolean enabled) {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(getPreferenceKey(descriptor), String.valueOf(enabled));
        PrefUtils.savePreferenceStore(store);
    }

    @NotNull
    private static String getPreferenceKey(@NotNull EasyConfigFeatureDescriptor descriptor) {
        return "easyConfig." + descriptor.getId();
    }
}
