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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgreServerTypeRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.postgresql.serverType"; //$NON-NLS-1$

    public synchronized static PostgreServerTypeRegistry getInstance() {
        if (instance == null) {
            instance = new PostgreServerTypeRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private static PostgreServerTypeRegistry instance = null;

    private final Map<String, PostgreServerType> serverTypes = new HashMap<>();

    private PostgreServerTypeRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            PostgreServerType type = new PostgreServerType(ext);
            serverTypes.put(type.getId(), type);
        }
    }

    public List<PostgreServerType> getServerTypes() {
        return new ArrayList<>(serverTypes.values());
    }

    public PostgreServerType getServerType(String id) {
        return serverTypes.get(id);
    }


}
