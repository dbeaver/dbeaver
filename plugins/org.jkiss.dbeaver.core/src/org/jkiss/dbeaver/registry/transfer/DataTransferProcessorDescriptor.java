/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
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
public class DataTransferProcessorDescriptor extends AbstractDescriptor
{
    private final DataTransferNodeDescriptor node;
    private final String id;
    private final ObjectType processorType;
    private final List<ObjectType> sourceTypes = new ArrayList<ObjectType>();
    private final String name;
    private final String description;
    private final DBPImage icon;
    private final List<DBPPropertyDescriptor> properties = new ArrayList<DBPPropertyDescriptor>();

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
