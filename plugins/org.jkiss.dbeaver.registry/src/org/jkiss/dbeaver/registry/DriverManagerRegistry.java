/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import java.util.ArrayList;
import java.util.List;

public class DriverManagerRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.driverManager"; //$NON-NLS-1$

    private static DriverManagerRegistry instance = null;

    private final List<DriverCategoryDescriptor> categories = new ArrayList<>();

    public synchronized static DriverManagerRegistry getInstance() {
        if (instance == null) {
            instance = new DriverManagerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private DriverManagerRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement viewElement : extElements) {
            if (viewElement.getName().equals("category")) {
                this.categories.add(
                    new DriverCategoryDescriptor(viewElement));
            }
        }
    }

    public List<DriverCategoryDescriptor> getCategories() {
        return new ArrayList<>(categories);
    }

    public DriverCategoryDescriptor getCategory(String id) {
        for (DriverCategoryDescriptor categoryDescriptor : categories) {
            if (id.equalsIgnoreCase(categoryDescriptor.getId())) {
                return categoryDescriptor;
            }
        }
        return null;
    }

}
