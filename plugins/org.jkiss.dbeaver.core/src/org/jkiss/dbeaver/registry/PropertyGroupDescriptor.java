/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPProperty;
import org.jkiss.dbeaver.model.DBPPropertyGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyGroupDescriptor
 */
public class PropertyGroupDescriptor implements DBPPropertyGroup
{
    public static final String PROPERTY_GROUP_TAG = "propertyGroup";

    private String name;
    private String description;
    private List<DBPProperty> properties = new ArrayList<DBPProperty>();

    public PropertyGroupDescriptor(IConfigurationElement config)
    {
        this.name = config.getAttribute("label");
        if (CommonUtils.isEmpty(this.name)) {
            this.name = "<undefined>";
        }
        this.description = config.getAttribute("description");
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.PROPERTY_TAG);
        for (IConfigurationElement prop : propElements) {
            addProperty(new PropertyDescriptor(this, prop));
        }
    }

    public PropertyGroupDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void addProperty(DBPProperty property)
    {
        properties.add(property);
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public List<DBPProperty> getProperties()
    {
        return properties;
    }
}