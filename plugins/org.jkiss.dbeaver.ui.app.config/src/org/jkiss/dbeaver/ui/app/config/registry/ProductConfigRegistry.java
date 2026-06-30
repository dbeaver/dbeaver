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
package org.jkiss.dbeaver.ui.app.config.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProductConfigRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.app.config";

    private static ProductConfigRegistry instance;

    private final List<ProductConfigPageDescriptor> pages;
    private final List<ProductConfigActionDescriptor> actions;

    private ProductConfigRegistry(@NotNull IExtensionRegistry registry) {
        var pages = new ArrayList<ProductConfigPageDescriptor>();
        var actions = new ArrayList<ProductConfigActionDescriptor>();

        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if ("page".equals(element.getName())) {
                pages.add(new ProductConfigPageDescriptor(element));
            } else if ("action".equals(element.getName())) {
                actions.add(new ProductConfigActionDescriptor(element));
            } else {
                throw new IllegalStateException("Unknown element " + element.getName());
            }
        }

        this.pages = pages.stream()
            .sorted(Comparator.comparingInt(ProductConfigPageDescriptor::getOrder))
            .toList();

        this.actions = actions.stream()
            .sorted(Comparator.comparing(ProductConfigActionDescriptor::getLabel))
            .toList();
    }

    @NotNull
    public static synchronized ProductConfigRegistry getInstance() {
        if (instance == null) {
            instance = new ProductConfigRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<ProductConfigPageDescriptor> getPages() {
        return pages;
    }

    @NotNull
    public List<ProductConfigActionDescriptor> getActions() {
        return actions;
    }
}
