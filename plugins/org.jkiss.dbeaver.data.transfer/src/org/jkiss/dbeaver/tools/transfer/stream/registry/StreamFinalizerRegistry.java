/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.*;

public class StreamFinalizerRegistry {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTransfer.streamFinalizer";

    private static StreamFinalizerRegistry instance = null;

    private final Map<String, StreamFinalizerDescriptor> finalizers = new HashMap<>();
    private final Map<String, StreamFinalizerConfiguratorDescriptor> configurators = new HashMap<>();

    public StreamFinalizerRegistry(@NotNull IExtensionRegistry registry) {
        final IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_ID);

        for (IConfigurationElement element : elements) {
            if ("finalizer".equals(element.getName())) {
                final StreamFinalizerDescriptor descriptor = new StreamFinalizerDescriptor(element);
                finalizers.put(descriptor.getId(), descriptor);
            }
            if ("configurator".equals(element.getName())) {
                final StreamFinalizerConfiguratorDescriptor descriptor = new StreamFinalizerConfiguratorDescriptor(element);
                configurators.put(descriptor.getId(), descriptor);
            }
        }
    }

    @NotNull
    public static synchronized StreamFinalizerRegistry getInstance() {
        if (instance == null) {
            instance = new StreamFinalizerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public Collection<StreamFinalizerDescriptor> getFinalizers() {
        return Collections.unmodifiableCollection(finalizers.values());
    }

    @Nullable
    public StreamFinalizerDescriptor getFinalizerById(@NotNull String id) {
        return finalizers.get(id);
    }

    @NotNull
    public Collection<StreamFinalizerConfiguratorDescriptor> getConfigurators() {
        return Collections.unmodifiableCollection(configurators.values());
    }

    @Nullable
    public StreamFinalizerConfiguratorDescriptor getConfiguratorById(@NotNull String id) {
        return configurators.get(id);
    }
}
