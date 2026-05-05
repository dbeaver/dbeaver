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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplicationFeature;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application feature registry
 */
public class ProductFeatureRegistry {

    private static final Log log = Log.getLog(ProductFeatureRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.productFeature"; //NON-NLS-1

    private final Map<String, DBPApplicationFeature> allFeatures = new LinkedHashMap<>();

    private static ProductFeatureRegistry instance = null;

    public synchronized static ProductFeatureRegistry getInstance() {
        if (instance == null) {
            instance = new ProductFeatureRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private ProductFeatureRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ObjectManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            String parentId = ext.getAttribute("parent");
            DBPApplicationFeature parent = CommonUtils.isEmpty(parentId) ? null : allFeatures.get(parentId);
            DBPApplicationFeature feature = new DBPApplicationFeature(
                parent,
                ext.getAttribute("id"),
                ext.getAttribute("label"),
                ext.getAttribute("description"));
            allFeatures.put(feature.getId(), feature);
        }
    }

    public List<DBPApplicationFeature> getAllFeatures() {
        return new ArrayList<>(allFeatures.values());
    }


}