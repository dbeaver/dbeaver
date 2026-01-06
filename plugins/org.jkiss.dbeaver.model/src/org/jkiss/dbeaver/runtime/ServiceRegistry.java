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
package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRegistry {
    private static final Log log = Log.getLog(ServiceRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.service"; //$NON-NLS-1$

    private static ServiceRegistry instance = null;

    private static class ServiceDescriptor extends AbstractDescriptor {

        private final ObjectType type;
        private final ObjectType impl;
        private final boolean headless;
        private final boolean singleton;
        private Object instance;

        ServiceDescriptor(IConfigurationElement config) {
            super(config);
            type = new ObjectType(config.getAttribute("name"));
            impl = new ObjectType(config.getAttribute("class"));
            this.headless = CommonUtils.toBoolean(config.getAttribute("headless"));
            this.singleton = CommonUtils.toBoolean(config.getAttribute("singleton"), true);
        }
    }

    public synchronized static ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, List<ServiceDescriptor>> services = new HashMap<>();

    private ServiceRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            ServiceDescriptor service = new ServiceDescriptor(ext);
            List<ServiceDescriptor> descriptors = services.computeIfAbsent(service.type.getImplName(), s -> new ArrayList<>());
            descriptors.add(service);
        }
    }

    @Nullable
    public <T> T getService(@NotNull Class<T> serviceType) {
        List<ServiceDescriptor> descriptors = services.get(serviceType.getName());
        if (!CommonUtils.isEmpty(descriptors)) {
            boolean headlessMode = DBWorkbench.getPlatform().getApplication().isHeadlessMode();
            for (ServiceDescriptor descriptor : descriptors) {
                if (descriptors.size() > 1 && headlessMode != descriptor.headless) {
                    continue;
                }
                if (!descriptor.singleton) {
                    try {
                        return descriptor.impl.createInstance(serviceType);
                    } catch (DBException e) {
                        log.debug(e);
                        return null;
                    }
                }
                if (descriptor.instance == null) {
                    try {
                        descriptor.instance = descriptor.impl.createInstance(Object.class);
                    } catch (DBException e) {
                        log.debug("Error creating service '" + serviceType.getName() + "'", e);
                    }
                }
                return (T) descriptor.instance;
            }
        }
        return null;
    }

}
