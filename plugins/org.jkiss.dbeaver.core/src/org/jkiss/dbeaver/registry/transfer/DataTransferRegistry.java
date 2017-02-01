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

package org.jkiss.dbeaver.registry.transfer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public class DataTransferRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTransfer"; //$NON-NLS-1$

    private static DataTransferRegistry instance = null;

    public synchronized static DataTransferRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataTransferRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<DataTransferNodeDescriptor> nodes = new ArrayList<>();

    private DataTransferRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (RegistryConstants.TAG_NODE.equals(ext.getName())) {
                nodes.add(new DataTransferNodeDescriptor(ext));
            }
        }
    }

    public List<DataTransferNodeDescriptor> getAvailableProducers(Collection<Class<?>> objectTypes)
    {
        return getAvailableNodes(DataTransferNodeDescriptor.NodeType.PRODUCER, objectTypes);
    }

    public List<DataTransferNodeDescriptor> getAvailableConsumers(Collection<Class<?>> objectTypes)
    {
        return getAvailableNodes(DataTransferNodeDescriptor.NodeType.CONSUMER, objectTypes);
    }

    List<DataTransferNodeDescriptor> getAvailableNodes(DataTransferNodeDescriptor.NodeType nodeType, Collection<Class<?>> objectTypes)
    {
        List<DataTransferNodeDescriptor> result = new ArrayList<>();
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getNodeType() == nodeType) {
                for (Class objectType : objectTypes) {
                    if (node.appliesToType(objectType)) {
                        result.add(node);
                        break;
                    }
                }
            }
        }
        return result;
    }

    public DataTransferNodeDescriptor getNodeByType(Class<? extends IDataTransferNode> type)
    {
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getNodeClass().equals(type)) {
                return node;
            }
        }
        return null;
    }

    public DataTransferNodeDescriptor getNodeById(String id)
    {
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
        }
        return null;
    }
}
