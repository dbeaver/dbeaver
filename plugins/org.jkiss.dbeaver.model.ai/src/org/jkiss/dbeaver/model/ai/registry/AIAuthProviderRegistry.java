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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.*;

public class AIAuthProviderRegistry {

    private static final Log log = Log.getLog(AIAuthProviderRegistry.class);
    private static AIAuthProviderRegistry instance;
    private final Map<String, List<DBAAuthProviderDescriptor>> authProviderByEngineID = new LinkedHashMap<>();
    private final Map<String, DBAAuthProviderDescriptor> authProviderByID = new HashMap<>();

    public static synchronized AIAuthProviderRegistry getInstance() {
        if (instance == null) {
            instance = new AIAuthProviderRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }


    @NotNull
    public List<DBAAuthProviderDescriptor> getEngineAuthProviders(@NotNull String engineID) {
        List<DBAAuthProviderDescriptor> descriptors = authProviderByEngineID.get(engineID);
        if (descriptors == null || descriptors.isEmpty()) {
            return List.of();
        }
        return descriptors;
    }

    @Nullable
    public DBAAuthProviderDescriptor getAuthProviderByID(@NotNull String id) {
        return authProviderByID.get(id);
    }

    private AIAuthProviderRegistry(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DBAAuthProviderDescriptor.EXTENSION_ID);

        for (IConfigurationElement ext : extElements) {
            if ("authProvider".equals(ext.getName())) {
                DBAAuthProviderDescriptor fd = new DBAAuthProviderDescriptor(ext);
                authProviderByEngineID.computeIfAbsent(fd.getEngineId(), k -> new ArrayList<>()).add(fd);
                authProviderByID.put(fd.getId(), fd);
            }
        }
    }

}
