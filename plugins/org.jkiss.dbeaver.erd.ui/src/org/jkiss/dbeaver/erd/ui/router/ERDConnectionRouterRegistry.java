/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.router;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ERDConnectionRouterRegistry {

    private Log log = Log.getLog(ERDNotationRegistry.class);
    private Map<String, ERDConnectionRouterDescriptor> connectionRouters = new LinkedHashMap<>();
    private static ERDConnectionRouterRegistry instance;
    private ERDConnectionRouterDescriptor defaultRouter;
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.erd.ui.routing"; //$NON-NLS-1$

    private ERDConnectionRouterRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] cfgElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement cfe : cfgElements) {
            try {
                addRouterConnection(new ERDConnectionRouterDescriptor(cfe));
            } catch (CoreException e) {
                log.error(e.getStatus());
            }
        }
    }

    private void addRouterConnection(@NotNull ERDConnectionRouterDescriptor descriptor) {
        if (connectionRouters.containsKey(descriptor.getId())) {
            log.error("ER Diagram Connection router is already defined for id:" + descriptor.getId());
            return;
        }
        connectionRouters.put(descriptor.getId(), descriptor);
        if (descriptor.isDefault()) {
            if (defaultRouter == null) {
                defaultRouter = descriptor;
            } else {
                log.error("The default ERD Connection router is already defined for id:" + defaultRouter.getId());
            }
        }
    }

    /**
     * Registry instance
     *
     * @return - registry instance
     */
    public static synchronized ERDConnectionRouterRegistry getInstance() {
        if (instance == null) {
            instance = new ERDConnectionRouterRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<ERDConnectionRouterDescriptor> getDescriptors() {
        return connectionRouters.values().stream().collect(Collectors.toList());
    }

    /*
     * Get connection router
     */
    @Nullable
    public ERDConnectionRouterDescriptor getConnectionRouter(String id) {
        if (!connectionRouters.containsKey(id)) {
            log.error("ERD Connection router is not defined for key:" + id);
            return null;
        }
        return connectionRouters.get(id);
    }

    /**
     * Get default descriptor
     *
     * @return - default descriptor
     */
    public ERDConnectionRouterDescriptor getDefaultRouter() {
        return defaultRouter;
    }

}
