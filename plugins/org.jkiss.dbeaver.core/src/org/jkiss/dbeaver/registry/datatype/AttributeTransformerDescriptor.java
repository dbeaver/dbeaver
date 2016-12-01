/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * AttributeTransformerDescriptor
 */
public class AttributeTransformerDescriptor extends DataTypeAbstractDescriptor<DBDAttributeTransformer> implements DBDAttributeTransformerDescriptor
{
    private final String name;
    private final String description;
    private boolean applyByDefault;
    private boolean custom;
    private final DBPImage icon;
    private List<DBPPropertyDescriptor> properties = new ArrayList<>();

    public AttributeTransformerDescriptor(IConfigurationElement config)
    {
        super(config, DBDAttributeTransformer.class);

        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.applyByDefault = "true".equals(config.getAttribute("applyByDefault"));
        this.custom = "true".equals(config.getAttribute("custom"));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));

        for (IConfigurationElement prop : config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isApplicableByDefault() {
        return applyByDefault;
    }

    @Override
    public boolean isCustom() {
        return custom;
    }

    @Override
    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

}