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

package org.jkiss.dbeaver.tools.transfer.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DataTransferRegistry
 */
public class DataTransferRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTransfer"; //$NON-NLS-1$

    private static DataTransferRegistry instance = null;

    private static final Log log = Log.getLog(DataTransferRegistry.class);

    public synchronized static DataTransferRegistry getInstance() {
        if (instance == null) {
            instance = new DataTransferRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DataTransferNodeDescriptor> nodes = new ArrayList<>();
    private final Map<String, DataTransferAttributeTransformerDescriptor> transformers = new LinkedHashMap<>();

    private DataTransferRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            // Load main nodes
            if ("node".equals(ext.getName())) {
                if (!CommonUtils.isEmpty(ext.getAttribute("ref"))) {
                    continue;
                }
                nodes.add(new DataTransferNodeDescriptor(ext));
            } else if ("transformer".equals(ext.getName())) {
                // Load transformers
                DataTransferAttributeTransformerDescriptor at = new DataTransferAttributeTransformerDescriptor(ext);
                transformers.put(at.getId(), at);
            }

        }

        // Load references
        for (IConfigurationElement ext : extElements) {
            if ("node".equals(ext.getName())) {
                String nodeReference = ext.getAttribute("ref");
                if (CommonUtils.isEmpty(nodeReference)) {
                    continue;
                }
                DataTransferNodeDescriptor refNode = getNodeById(nodeReference);
                if (refNode == null) {
                    log.error("Referenced data transfer node '" + nodeReference + "' not found");
                } else {
                    refNode.loadNodeConfigurations(ext);
                }
            }
        }
        nodes.sort(Comparator.comparing(DataTransferNodeDescriptor::getName));

        //transformers.sort(Comparator.comparing(DataTransferAttributeTransformerDescriptor::getName));
    }

    public List<DataTransferNodeDescriptor> getAvailableProducers(Collection<DBSObject> sourceObjects) {
        return getAvailableNodes(DataTransferNodeDescriptor.NodeType.PRODUCER, sourceObjects);
    }

    public List<DataTransferNodeDescriptor> getAvailableConsumers(Collection<DBSObject> sourceObjects) {
        return getAvailableNodes(DataTransferNodeDescriptor.NodeType.CONSUMER, sourceObjects);
    }

    List<DataTransferNodeDescriptor> getAvailableNodes(DataTransferNodeDescriptor.NodeType nodeType, Collection<DBSObject> sourceObjects) {
        List<DataTransferNodeDescriptor> result = new ArrayList<>();
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getNodeType() == nodeType) {
                for (DBSObject sourceObject : sourceObjects) {
                    if (node.appliesToType(sourceObject.getClass())) {
                        result.add(node);
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<DataTransferNodeDescriptor> getNodes(DataTransferNodeDescriptor.NodeType nodeType) {
        List<DataTransferNodeDescriptor> result = new ArrayList<>();
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getNodeType() == nodeType) {
                result.add(node);
            }
        }
        return result;
    }

    public DataTransferNodeDescriptor getNodeByType(Class<? extends IDataTransferNode> type) {
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getNodeClass().equals(type)) {
                return node;
            }
        }
        return null;
    }

    public DataTransferNodeDescriptor getNodeById(String id) {
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
        }
        return null;
    }

    public DataTransferProcessorDescriptor getProcessor(String processorFullId) {
        String[] idParts = processorFullId.split(":");
        if (idParts.length == 2) {
            DataTransferNodeDescriptor node = getNodeById(idParts[0]);
            if (node != null) {
                return node.getProcessor(idParts[1]);
            }
        }
        return null;
    }

    @Nullable
    public List<DataTransferProcessorDescriptor> getAvailableProcessors(Class<? extends IDataTransferNode> nodeType, Class<?> objectType) {
        List<DataTransferProcessorDescriptor> processors = null;
        for (DataTransferNodeDescriptor node : nodes) {
            if (node.getNodeClass() == nodeType) {
                if (node.appliesToType(objectType)) {
                    return node.getAvailableProcessors(objectType);
                }
            }
        }
        return null;
    }

    @NotNull
    public List<DataTransferAttributeTransformerDescriptor> getAttributeTransformers() {
        return new ArrayList<>(transformers.values());
    }

    @Nullable
    public DataTransferAttributeTransformerDescriptor getAttributeTransformer(String id) {
        return transformers.get(id);
    }

    public DataTransferAttributeTransformerDescriptor getAttributeTransformerByName(String tName) {
        return transformers.values().stream().filter(t -> t.getName().equals(tName)).findFirst().orElse(null);
    }
}
