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
import org.eclipse.swt.graphics.Image;

/**
 * DataSourceViewDescriptor
 */
public class DataSourceViewDescriptor extends AbstractDescriptor
{
    private String id;
    private String targetID;
    private String label;
    private ObjectType viewType;
    private Image icon;

    public DataSourceViewDescriptor(DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        super(provider.getPluginId());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.targetID = config.getAttribute(RegistryConstants.ATTR_TARGET_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.viewType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
    }

    public String getId()
    {
        return id;
    }

    public String getTargetID()
    {
        return targetID;
    }

    public String getLabel()
    {
        return label;
    }

    public Image getIcon()
    {
        return icon;
    }

    public <T> T createView(Class<T> implementsClass)
    {
        try {
            return viewType.createInstance(implementsClass);
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Can't create view '" + viewType.getImplName() + "'", ex);
        }
    }
}
