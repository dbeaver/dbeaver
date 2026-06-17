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
package org.jkiss.dbeaver.ui.config.easy.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EasyConfigRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.easyConfig";

    private static EasyConfigRegistry instance;

    private final List<EasyConfigPageDescriptor> pages;
    private final List<EasyConfigActionDescriptor> actions;

    private EasyConfigRegistry(@NotNull IExtensionRegistry registry) {
        var pages = new ArrayList<EasyConfigPageDescriptor>();
        var actions = new ArrayList<EasyConfigActionDescriptor>();

        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if ("page".equals(element.getName())) {
                pages.add(new EasyConfigPageDescriptor(element));
            } else if ("action".equals(element.getName())) {
                actions.add(new EasyConfigActionDescriptor(element));
            } else {
                throw new IllegalStateException("Unknown element " + element.getName());
            }
        }

        this.pages = pages.stream()
            .sorted(Comparator.comparingInt(EasyConfigPageDescriptor::getOrder))
            .toList();

        this.actions = actions.stream()
            .sorted(Comparator.comparing(EasyConfigActionDescriptor::getLabel))
            .toList();
    }

    @NotNull
    public static synchronized EasyConfigRegistry getInstance() {
        if (instance == null) {
            instance = new EasyConfigRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<EasyConfigPageDescriptor> getPages() {
        return pages;
    }

    @NotNull
    public List<EasyConfigActionDescriptor> getActions() {
        return actions;
    }
}
