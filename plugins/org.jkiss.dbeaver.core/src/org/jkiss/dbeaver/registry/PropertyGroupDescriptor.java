/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    public static final String TAG_PROPERTY_GROUP = "propertyGroup"; //NON-NLS-1
    public static final String ATTR_LABEL = "label"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String NAME_UNDEFINED = "<undefined>"; //NON-NLS-1

    private String name;
    private String description;
    private List<DBPProperty> properties = new ArrayList<DBPProperty>();

    public PropertyGroupDescriptor(IConfigurationElement config)
    {
        this.name = config.getAttribute(ATTR_LABEL);
        if (CommonUtils.isEmpty(this.name)) {
            this.name = NAME_UNDEFINED;
        }
        this.description = config.getAttribute(ATTR_DESCRIPTION);
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY);
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