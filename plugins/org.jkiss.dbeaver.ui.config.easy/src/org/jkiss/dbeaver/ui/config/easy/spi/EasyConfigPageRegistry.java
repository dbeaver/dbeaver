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
package org.jkiss.dbeaver.ui.config.easy.spi;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public final class EasyConfigPageRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.easyConfig";

    private static EasyConfigPageRegistry instance;

    private final Collection<EasyConfigPageDescriptor> pages = new TreeSet<>(Comparator.comparingInt(EasyConfigPageDescriptor::getOrder));

    private EasyConfigPageRegistry(@NotNull IExtensionRegistry registry) {
        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if ("page".equals(element.getName())) {
                pages.add(new EasyConfigPageDescriptor(element));
            }
        }
    }

    @NotNull
    public static synchronized EasyConfigPageRegistry getInstance() {
        if (instance == null) {
            instance = new EasyConfigPageRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public Collection<EasyConfigPageDescriptor> getPages() {
        return pages;
    }
}
