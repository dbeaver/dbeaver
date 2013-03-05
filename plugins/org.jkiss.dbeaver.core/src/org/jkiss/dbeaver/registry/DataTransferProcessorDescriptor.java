/*
 * Copyright (C) 2010-2012 Serge Rieder
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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferProcessorDescriptor
 */
public class DataTransferProcessorDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTransfer"; //$NON-NLS-1$

    private String id;
    private ObjectType exporterType;
    private List<ObjectType> sourceTypes = new ArrayList<ObjectType>();
    private String name;
    private String description;
    private String fileExtension;
    private Image icon;
    private List<IPropertyDescriptor> properties = new ArrayList<IPropertyDescriptor>();
    private DBDDisplayFormat exportFormat;

    public DataTransferProcessorDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.exporterType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.fileExtension = config.getAttribute(RegistryConstants.ATTR_EXTENSION);
        String formatName = config.getAttribute(RegistryConstants.ATTR_FORMAT);
        if (CommonUtils.isEmpty(formatName)) {
            exportFormat = DBDDisplayFormat.UI;
        } else {
            exportFormat = DBDDisplayFormat.valueOf(formatName.toUpperCase());
        }
        String iconPath = config.getAttribute(RegistryConstants.ATTR_ICON);
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }

        for (IConfigurationElement typeCfg : CommonUtils.safeArray(config.getChildren(RegistryConstants.ATTR_SOURCE_TYPE))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute(RegistryConstants.ATTR_TYPE)));
        }

        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP);
        for (IConfigurationElement prop : propElements) {
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

    public String getFileExtension()
    {
        return fileExtension;
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
        try {
            Class<IDataTransferProcessor> clazz = exporterType.getObjectClass(IDataTransferProcessor.class);
            if (clazz == null) {
                throw new InstantiationException("Cannot find exporter class " + exporterType.implName);
            }
            return clazz.newInstance();
        } catch (Exception e) {
            throw new DBException("Can't instantiate data exporter", e);
        }
    }

    public DBDDisplayFormat getExportFormat()
    {
        return exportFormat;
    }
}
