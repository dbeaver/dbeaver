/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DataTransferNodeDescriptor
 */
public class DataTransferNodeDescriptor extends AbstractDescriptor
{
    public enum NodeType {
        PRODUCER,
        CONSUMER
    }

    @NotNull
    private final String id;
    @NotNull
    private final String name;
    private final String description;
    @NotNull
    private final DBPImage icon;
    private final NodeType nodeType;
    private final ObjectType implType;
    private final ObjectType settingsType;
    private final List<ObjectType> sourceTypes = new ArrayList<>();
    private final List<DataTransferProcessorDescriptor> processors = new ArrayList<>();

    public DataTransferNodeDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"), DBIcon.TYPE_UNKNOWN);
        this.nodeType = CommonUtils.valueOf(NodeType.class, config.getAttribute("type").toUpperCase(Locale.ENGLISH), NodeType.PRODUCER);
        this.implType = new ObjectType(config.getAttribute("class"));
        this.settingsType = new ObjectType(config.getAttribute("settings"));

        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren("sourceType"))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute("type")));
        }

        loadNodeConfigurations(config);
    }

    void loadNodeConfigurations(IConfigurationElement config) {
        List<DataTransferProcessorDescriptor> procList = new ArrayList<>();
        for (IConfigurationElement processorConfig : ArrayUtils.safeArray(config.getChildren("processor"))) {
            procList.add(new DataTransferProcessorDescriptor(this, processorConfig));
        }
        procList.sort(Comparator.comparing(DataTransferProcessorDescriptor::getName));
        this.processors.addAll(procList);
    }

    @NotNull
    public String getId()
    {
        return id;
    }

    @NotNull
    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    @NotNull
    public DBPImage getIcon()
    {
        return icon;
    }

    public Class<? extends IDataTransferNode> getNodeClass()
    {
        return implType.getObjectClass(IDataTransferNode.class);
    }

    public IDataTransferNode createNode() throws DBException
    {
        implType.checkObjectClass(IDataTransferNode.class);
        try {
            return implType.getObjectClass(IDataTransferNode.class).getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create data transformer node", e);
        }
    }

    public IDataTransferSettings createSettings() throws DBException
    {
        settingsType.checkObjectClass(IDataTransferSettings.class);
        try {
            return settingsType.getObjectClass(IDataTransferSettings.class).getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create node settings", e);
        }
    }

    public NodeType getNodeType()
    {
        return nodeType;
    }

    public boolean appliesToType(Class objectType)
    {
        if (!sourceTypes.isEmpty()) {
            for (ObjectType sourceType : sourceTypes) {
                if (sourceType.matchesType(objectType)) {
                    return true;
                }
            }
        }
        for (DataTransferProcessorDescriptor processor : processors) {
            if (processor.appliesToType(objectType)) {
                return true;
            }
        }
        return false;
    }

    public DataTransferProcessorDescriptor[] getProcessors() {
        return processors.toArray(new DataTransferProcessorDescriptor[0]);
    }

    /**
     * Returns data exporter which supports ALL specified object types
     * @param sourceObjects object types
     * @return list of editors
     */
    public List<DataTransferProcessorDescriptor> getAvailableProcessors(Collection<DBSObject> sourceObjects) {
        List<DataTransferProcessorDescriptor> editors = new ArrayList<>();
        for (DataTransferProcessorDescriptor descriptor : processors) {
            boolean supports = true;
            for (DBSObject sourceObject : sourceObjects) {
                if (!descriptor.appliesToType(sourceObject.getClass())) {
                    boolean adapts = false;
                    if (sourceObject instanceof IAdaptable) {
                        if (descriptor.adaptsToType((IAdaptable)sourceObject)) {
                            adapts = true;
                        }
                    }
                    if (!adapts) {
                        supports = false;
                        break;
                    }
                }
            }
            if (supports) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

    public List<DataTransferProcessorDescriptor> getAvailableProcessors(Class<?> objectType) {
        List<DataTransferProcessorDescriptor> procList = new ArrayList<>();
        for (DataTransferProcessorDescriptor descriptor : this.processors) {
            if (descriptor.appliesToType(objectType)) {
                procList.add(descriptor);
            }
        }
        return procList;
    }

    public DataTransferProcessorDescriptor getProcessor(String id)
    {
        for (DataTransferProcessorDescriptor descriptor : processors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}
