/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

/**
 * ToolDescriptor
 */
public class ToolDescriptor extends AbstractContextDescriptor {
    private final String id;
    private final String label;
    private final String description;
    private final String toolClassName;
    private final Image icon;
    private final boolean singleton;
    private final ToolGroupDescriptor group;

    public ToolDescriptor(
        DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        super(provider.getPluginId(), config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.toolClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.singleton = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_SINGLETON));
        String groupId = config.getAttribute(RegistryConstants.ATTR_GROUP);
        this.group = CommonUtils.isEmpty(groupId) ? null : provider.getToolGroup(groupId);
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
        Class<IExternalTool> toolClass = getObjectClass(toolClassName, IExternalTool.class);
        if (toolClass == null) {
            throw new DBException("Tool class '" + toolClassName + "' not found");
        }
        try {
            return toolClass.newInstance();
        } catch (Throwable ex) {
            throw new DBException("Can't create tool '" + toolClassName + "'", ex);
        }
    }

}
