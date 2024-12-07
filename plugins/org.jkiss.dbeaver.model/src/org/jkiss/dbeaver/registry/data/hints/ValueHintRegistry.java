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

package org.jkiss.dbeaver.registry.data.hints;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.data.hints.standard.VoidHintProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * ValueHintRegistry
 */
public class ValueHintRegistry extends AbstractValueBindingRegistry<DBDValueHintProvider, ValueHintProviderDescriptor> {

    private static ValueHintRegistry instance = null;

    public synchronized static ValueHintRegistry getInstance() {
        if (instance == null) {
            instance = new ValueHintRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ValueHintProviderDescriptor> descriptors = new ArrayList<>();

    private ValueHintRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ValueHintProviderDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ValueHintProviderDescriptor.TAG_HINT_PROVIDER.equals(ext.getName())) {
                descriptors.add(new ValueHintProviderDescriptor(ext));
            }
        }
    }

    @NotNull
    @Override
    protected List<ValueHintProviderDescriptor> getDescriptors() {
        return descriptors;
    }

    @NotNull
    @Override
    protected DBDValueHintProvider getDefaultValueBinding() {
        return VoidHintProvider.INSTANCE;
    }
}
