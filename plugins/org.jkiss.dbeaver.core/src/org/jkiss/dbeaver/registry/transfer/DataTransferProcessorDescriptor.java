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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.app.DBPRegistryDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferProcessorDescriptor
 */
public class DataTransferProcessorDescriptor extends AbstractDescriptor implements DBPRegistryDescriptor<IDataTransferProcessor>
{
    private final DataTransferNodeDescriptor node;
    private final String id;
    private final ObjectType processorType;
    private final List<ObjectType> sourceTypes = new ArrayList<>();
    private final String name;
    private final String description;
    @NotNull
    private final DBPImage icon;
    private final List<DBPPropertyDescriptor> properties = new ArrayList<>();

    public DataTransferProcessorDescriptor(DataTransferNodeDescriptor node, IConfigurationElement config)
    {
        super(config);
        this.node = node;
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.processorType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON), DBIcon.TYPE_UNKNOWN);

        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren(RegistryConstants.ATTR_SOURCE_TYPE))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute(RegistryConstants.ATTR_TYPE)));
        }

        for (IConfigurationElement prop : ArrayUtils.safeArray(config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
    }

    public String getId()
    {
        return id;
    }

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

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public boolean appliesToType(Class objectType)
    {
        if (sourceTypes.isEmpty()) {
            return true;
        }
        for (ObjectType sourceType : sourceTypes) {
            if (sourceType.matchesType(objectType)) {
                return true;
            }
        }
        return false;
    }

    public IDataTransferProcessor getInstance()
    {
        try {
            processorType.checkObjectClass(IDataTransferProcessor.class);
            Class<? extends IDataTransferProcessor> clazz = processorType.getObjectClass(IDataTransferProcessor.class);
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can't instantiate data exporter", e);
        }
    }

    public DataTransferNodeDescriptor getNode()
    {
        return node;
    }
}
