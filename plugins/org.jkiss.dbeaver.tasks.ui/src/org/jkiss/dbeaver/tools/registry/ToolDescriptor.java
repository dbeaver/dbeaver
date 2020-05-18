/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.tools.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
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

    public IUserInterfaceTool createTool()
        throws DBException
    {
        return toolType.createInstance(IUserInterfaceTool.class);
    }

    @Override
    public String toString() {
        return id + " (" + label + ")";
    }
}
