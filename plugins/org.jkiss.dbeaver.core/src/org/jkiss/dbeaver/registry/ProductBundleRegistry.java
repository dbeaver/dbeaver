/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ProductBundleRegistry
 */
public class ProductBundleRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.product.bundles"; //$NON-NLS-1$

    private static ProductBundleRegistry instance = null;

    public synchronized static ProductBundleRegistry getInstance()
    {
        if (instance == null) {
            instance = new ProductBundleRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private Map<String, ProductBundleDescriptor> bundles = new LinkedHashMap<>();

    private ProductBundleRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("bundle".equals(ext.getName())) {
                ProductBundleDescriptor bundle = new ProductBundleDescriptor(ext);
                bundles.put(bundle.getId(), bundle);
            }
        }
    }

    public boolean hasBundle(String id)
    {
        return bundles.containsKey(id);
    }

    public ProductBundleDescriptor getBundle(String id)
    {
        return bundles.get(id);
    }

}
