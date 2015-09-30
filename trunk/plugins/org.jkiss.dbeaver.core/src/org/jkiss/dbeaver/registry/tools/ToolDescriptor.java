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

package org.jkiss.dbeaver.registry.tools;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

/**
 * ToolDescriptor
 */
public class ToolDescriptor extends AbstractContextDescriptor {
    private final String id;
    private final String label;
    private final String description;
    private final ObjectType toolType;
    private final DBPImage icon;
    private final boolean singleton;
    private final ToolGroupDescriptor group;

    public ToolDescriptor(IConfigurationElement config)
    {
        super(config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.toolType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.singleton = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_SINGLETON));
        String groupId = config.getAttribute(RegistryConstants.ATTR_GROUP);
        this.group = CommonUtils.isEmpty(groupId) ? null : ToolsRegistry.getInstance().getToolGroup(groupId);
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

    public DBPImage getIcon() {
        return icon;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public ToolGroupDescriptor getGroup() {
        return group;
    }

    @Override
    protected Object adaptType(DBPObject object) {
        if (object instanceof DBSObject) {
            return ((DBSObject) object).getDataSource();
        }
        return super.adaptType(object);
    }

    public IExternalTool createTool()
        throws DBException
    {
        return toolType.createInstance(IExternalTool.class);
    }

}
