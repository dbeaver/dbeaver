/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.tools.transfer.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.dbeaver.Log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DataTransferConfiguratorRegistry
 */
public class DataTransferConfiguratorRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTransferConfigurator"; //$NON-NLS-1$

    private static DataTransferConfiguratorRegistry instance = null;

    private static final Log log = Log.getLog(DataTransferConfiguratorRegistry.class);

    public synchronized static DataTransferConfiguratorRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataTransferConfiguratorRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private Map<String, DataTransferNodeConfiguratorDescriptor> nodeConfigurators = new LinkedHashMap<>();

    private DataTransferConfiguratorRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            // Load main nodeConfigurators
            if ("configPages".equals(ext.getName())) {
                String nodeId = ext.getAttribute("node");
                DataTransferNodeConfiguratorDescriptor descriptor = nodeConfigurators.get(nodeId);
                if (descriptor == null) {
                    descriptor = new DataTransferNodeConfiguratorDescriptor(ext);
                    nodeConfigurators.put(nodeId, descriptor);
                } else {
                    descriptor.loadNodeConfigurations(ext);
                }
            }
        }
    }

    public DataTransferNodeConfiguratorDescriptor getConfigurator(String nodeId) {
        return nodeConfigurators.get(nodeId);
    }

    public DataTransferPageDescriptor getPageDescriptor(IWizardPage page) {
        for (DataTransferNodeConfiguratorDescriptor nd : nodeConfigurators.values()) {
            for (DataTransferPageDescriptor pd : nd.patPageDescriptors()) {
                if (pd.getPageClass().getImplName().equals(page.getClass().getName())) {
                    return pd;
                }
            }
        }
        return null;
    }

}
