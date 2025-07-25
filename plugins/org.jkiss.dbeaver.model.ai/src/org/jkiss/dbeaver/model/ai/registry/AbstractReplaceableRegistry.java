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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic registry that supports the {@code replaces="otherId"} attribute.
 * Sub-classes only have to implement {@link #createDescriptor(IConfigurationElement)}
 * and expose a public <i>get-something</i> method that delegates to {@link #get(String)}.
 */
public abstract class AbstractReplaceableRegistry<T,
    D extends AbstractReplaceableDescriptor<T>> {

    private final Map<String, D> descriptors  = new LinkedHashMap<>();
    private final Map<String, String> replaces = new LinkedHashMap<>();

    protected AbstractReplaceableRegistry(
        IExtensionRegistry registry,
        String extensionPoint,
        String elementName
    ) {
        for (IConfigurationElement el : registry.getConfigurationElementsFor(extensionPoint)) {
            if (!elementName.equals(el.getName())) {
                continue;
            }
            D descriptor = createDescriptor(el);
            descriptors.put(descriptor.getId(), descriptor);

            String rep = descriptor.getReplaces();
            if (!CommonUtils.isEmpty(rep)) {
                for (String id : rep.split(",")) {
                    replaces.put(id.trim(), descriptor.getId());
                }
            }
        }
    }

    protected abstract D createDescriptor(IConfigurationElement cfg);

    /** Returns an instance resolving the whole {@code replaces} chain. */
    public T get(String id) throws DBException {
        while (replaces.containsKey(id)) {
            id = replaces.get(id);
        }
        D descriptor = descriptors.get(id);
        if (descriptor == null) {
            throw new DBException(
                "Registry element '" + id + "' not found in " + getClass().getSimpleName());
        }
        return descriptor.createInstance();
    }
}
