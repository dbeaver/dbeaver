/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.registry.transfer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferProcessorDescriptor
 */
public class DataTransferProcessorDescriptor extends AbstractDescriptor
{
    private final DataTransferNodeDescriptor node;
    private final String id;
    private final ObjectType processorType;
    private final List<ObjectType> sourceTypes = new ArrayList<ObjectType>();
    private final String name;
    private final String description;
    private final Image icon;
    private final List<IPropertyDescriptor> properties = new ArrayList<IPropertyDescriptor>();

    public DataTransferProcessorDescriptor(DataTransferNodeDescriptor node, IConfigurationElement config)
    {
        super(config);
        this.node = node;
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.processorType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));

        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren(RegistryConstants.ATTR_SOURCE_TYPE))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute(RegistryConstants.ATTR_TYPE)));
        }

        for (IConfigurationElement prop : ArrayUtils.safeArray(config.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP))) {
            properties.addAll(PropertyDescriptorEx.extractProperties(prop));
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

    public Image getIcon()
    {
        return icon;
    }

    public List<IPropertyDescriptor> getProperties() {
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

    public IDataTransferProcessor createProcessor() throws DBException
    {
        processorType.checkObjectClass(IDataTransferProcessor.class);
        try {
            Class<? extends IDataTransferProcessor> clazz = processorType.getObjectClass(IDataTransferProcessor.class);
            return clazz.newInstance();
        } catch (Exception e) {
            throw new DBException("Can't instantiate data exporter", e);
        }
    }

    public DataTransferNodeDescriptor getNode()
    {
        return node;
    }
}
