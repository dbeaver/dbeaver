/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class WorkbenchHandlerRegistry
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.workbenchHandler"; //$NON-NLS-1$
    public static final String WORKBENCH_WINDOW_INITIALIZER = "workbenchWindowInitializer";

    private static WorkbenchHandlerRegistry instance = null;

    public static class InitializerDescriptor extends AbstractDescriptor {
        private final ObjectType type;
        private final int order;

        private InitializerDescriptor(IConfigurationElement config) {
            super(config);
            type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
            order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER), Integer.MAX_VALUE);
        }

        public IWorkbenchWindowInitializer newInstance() throws DBException {
            return type.createInstance(IWorkbenchWindowInitializer.class);
        }

        public int getOrder() {
            return order;
        }
    }

    public synchronized static WorkbenchHandlerRegistry getInstance()
    {
        if (instance == null) {
            instance = new WorkbenchHandlerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<InitializerDescriptor> initializers;

    private WorkbenchHandlerRegistry(IExtensionRegistry registry)
    {
        initializers = Arrays.stream(registry.getConfigurationElementsFor(EXTENSION_ID))
            .filter(ext -> ext.getName().equals(WORKBENCH_WINDOW_INITIALIZER))
            .map(InitializerDescriptor::new)
            .sorted(Comparator.comparingInt(InitializerDescriptor::getOrder))
            .toList();
    }

    @NotNull
    public Collection<InitializerDescriptor> getWorkbenchWindowInitializers() {
        return initializers;
    }
}
