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

package org.jkiss.dbeaver.registry.formatter;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * DataFormatterDescriptor
 */
public class DataFormatterDescriptor extends AbstractDescriptor
{
    static final Log log = Log.getLog(DataFormatterDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataFormatter"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private List<PropertyDescriptor> properties = new ArrayList<>();
    private DBDDataFormatterSample sample;
    private ObjectType formatterType;

    public DataFormatterDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.formatterType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);

        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP);
        for (IConfigurationElement prop : propElements) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
        Class<?> objectClass = getObjectClass(config.getAttribute(RegistryConstants.ATTR_SAMPLE_CLASS));
        try {
            sample = (DBDDataFormatterSample)objectClass.newInstance();
        } catch (Exception e) {
            log.error("Can't instantiate data formatter '" + getId() + "' sample");
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

    public List<PropertyDescriptor> getProperties() {
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
