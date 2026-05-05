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

package org.jkiss.dbeaver.tools.transfer.ui.registry;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.dbeaver.Log;

import java.util.*;

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

    private DataTransferConfiguratorRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        Map<String, DataTransferNodeConfiguratorDescriptor> pagesById = new HashMap<>();
        // Remember all replacement pairs to apply in a second pass
        List<Map.Entry<String, DataTransferNodeConfiguratorDescriptor>> replacements = new ArrayList<>();

        Arrays.stream(registry.getConfigurationElementsFor(EXTENSION_ID))
            .filter(it -> "configPages".equals(it.getName()))
            .forEach(it -> {
                String nodeId = it.getAttribute("node");
                String replaces = it.getAttribute("replaces");
                DataTransferNodeConfiguratorDescriptor descriptor = new DataTransferNodeConfiguratorDescriptor(it);
                if (pagesById.containsKey(nodeId)) {
                    throw new IllegalArgumentException("Duplicate node id: " + nodeId);
                }

                pagesById.put(nodeId, descriptor);

                if (replaces != null && !replaces.isEmpty()) {
                    replacements.add(new AbstractMap.SimpleEntry<>(replaces, descriptor));
                }
            });

        replacements.forEach(replacement -> {
            pagesById.remove(replacement.getValue().getId());
            DataTransferNodeConfiguratorDescriptor replaced = pagesById.put(replacement.getKey(), replacement.getValue());
            if (replaced != null) {
                log.debug("Data transfer configurator '" + replaced.getId() + "' is replaced by '" + replacement.getValue().getId() + "'");
            } else {
                log.debug(
                    "No configurator with id '" + replacement.getKey()
                        + "' found to replace, adding new one: " + replacement.getValue().getId()
                );
            }
        });

        nodeConfigurators.putAll(pagesById);
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
