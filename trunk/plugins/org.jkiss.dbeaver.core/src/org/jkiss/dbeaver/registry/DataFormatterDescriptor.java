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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;

import java.util.ArrayList;
import java.util.List;

/**
 * DataFormatterDescriptor
 */
public class DataFormatterDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataFormatter"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private List<PropertyDescriptorEx> properties = new ArrayList<PropertyDescriptorEx>();
    private DBDDataFormatterSample sample;
    private ObjectType formatterType;

    public DataFormatterDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.formatterType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);

        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP);
        for (IConfigurationElement prop : propElements) {
            properties.addAll(PropertyDescriptorEx.extractProperties(prop));
        }
        Class<?> objectClass = getObjectClass(config.getAttribute(RegistryConstants.ATTR_SAMPLE_CLASS));
        try {
            sample = (DBDDataFormatterSample)objectClass.newInstance();
        } catch (Exception e) {
            log.error("Could not instantiate data formatter '" + getId() + "' sample");
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

    public DBDDataFormatterSample getSample()
    {
        return sample;
    }

    public List<PropertyDescriptorEx> getProperties() {
        return properties;
    }

    public DBDDataFormatter createFormatter() throws IllegalAccessException, InstantiationException
    {
        Class<? extends DBDDataFormatter> clazz = formatterType.getObjectClass(DBDDataFormatter.class);
        if (clazz == null) {
            return null;
        }
        return clazz.newInstance();
    }

}
