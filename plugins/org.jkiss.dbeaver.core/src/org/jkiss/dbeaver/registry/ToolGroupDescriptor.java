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
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * ToolDescriptor
 */
public class ToolGroupDescriptor extends AbstractContextDescriptor {
    private final String id;
    private final String label;
    private final String description;
    private final Image icon;
    private final ToolGroupDescriptor parent;

    public ToolGroupDescriptor(
        DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        super(config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        String parentId = config.getAttribute(RegistryConstants.ATTR_PARENT);
        this.parent = CommonUtils.isEmpty(parentId) ? null : provider.getToolGroup(parentId);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Image getIcon() {
        return icon;
    }

    public ToolGroupDescriptor getParent() {
        return parent;
    }

    @Override
    protected Object adaptType(DBPObject object) {
        if (object instanceof DBSObject) {
            return ((DBSObject) object).getDataSource();
        }
        return super.adaptType(object);
    }

}
