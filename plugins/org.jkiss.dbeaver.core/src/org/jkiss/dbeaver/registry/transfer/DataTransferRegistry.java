/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
