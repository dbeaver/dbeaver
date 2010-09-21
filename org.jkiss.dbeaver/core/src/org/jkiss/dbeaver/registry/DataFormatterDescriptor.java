/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * DataFormatterDescriptor
 */
public class DataFormatterDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataFormatter";

    private String id;
    private String className;
    private String name;
    private String description;
    private List<PropertyGroupDescriptor> propertyGroups = new ArrayList<PropertyGroupDescriptor>();

    private Class<?> formatterClass;

    public DataFormatterDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");

        IConfigurationElement[] propElements = config.getChildren(PropertyGroupDescriptor.PROPERTY_GROUP_TAG);
        for (IConfigurationElement prop : propElements) {
            propertyGroups.add(new PropertyGroupDescriptor(prop));
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

    public List<PropertyGroupDescriptor> getPropertyGroups() {
        return propertyGroups;
    }

    public Class getFormatterClass()
    {
        if (formatterClass == null) {
            formatterClass = getObjectClass(className);
        }
        return formatterClass;
    }

    public DBDDataFormatter createFormatter() throws IllegalAccessException, InstantiationException
    {
        Class clazz = getFormatterClass();
        if (clazz == null) {
            return null;
        }
        return (DBDDataFormatter)clazz.newInstance();
    }

}
