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
package org.jkiss.dbeaver.registry.language;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlatformLanguageRegistry
{
    private static PlatformLanguageRegistry instance = null;

    public synchronized static PlatformLanguageRegistry getInstance()
    {
        if (instance == null) {
            instance = new PlatformLanguageRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<PlatformLanguageDescriptor> descriptors = new ArrayList<>();

    private PlatformLanguageRegistry(IExtensionRegistry registry)
    {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(PlatformLanguageDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                PlatformLanguageDescriptor formatterDescriptor = new PlatformLanguageDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }
        }
    }

    public List<PlatformLanguageDescriptor> getLanguages()
    {
        return descriptors;
    }
    
    public PlatformLanguageDescriptor getLanguage(String id)
    {
        for (PlatformLanguageDescriptor descriptor : descriptors) {
            if (descriptor.getCode().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public PlatformLanguageDescriptor getLanguage(Locale locale)
    {
        for (PlatformLanguageDescriptor descriptor : descriptors) {
            if (descriptor.getCode().equals(locale.getLanguage())) {
                return descriptor;
            }
        }
        return null;
    }

}
