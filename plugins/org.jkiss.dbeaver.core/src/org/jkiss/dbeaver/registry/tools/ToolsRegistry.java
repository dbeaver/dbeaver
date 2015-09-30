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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolsRegistry
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.tools"; //$NON-NLS-1$

    static final String TAG_TOOLS = "tools"; //$NON-NLS-1$
    static final String TAG_TOOL = "tool"; //$NON-NLS-1$
    static final String TAG_TOOL_GROUP = "toolGroup"; //$NON-NLS-1$

    private static ToolsRegistry instance = null;

    public synchronized static ToolsRegistry getInstance()
    {
        if (instance == null) {
            instance = new ToolsRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, ToolGroupDescriptor> toolGroups = new LinkedHashMap<String, ToolGroupDescriptor>();
    private final List<ToolDescriptor> tools = new ArrayList<ToolDescriptor>();

    private ToolsRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement toolsElement : extConfigs) {
            // Load tools
            {
                for (IConfigurationElement toolElement : toolsElement.getChildren(TAG_TOOL_GROUP)) {
                    ToolGroupDescriptor group = new ToolGroupDescriptor(toolElement);
                    this.toolGroups.put(group.getId(), group);
                }
                for (IConfigurationElement toolElement : toolsElement.getChildren(TAG_TOOL)) {
                    this.tools.add(
                        new ToolDescriptor(toolElement));
                }
            }
        }
    }

    public void dispose()
    {
        tools.clear();
        toolGroups.clear();
    }

    ToolGroupDescriptor getToolGroup(String id) {
        return toolGroups.get(id);
    }

    public List<ToolDescriptor> getTools(IStructuredSelection selection)
    {
        List<DBSObject> objects = NavigatorUtils.getSelectedObjects(selection);
        List<ToolDescriptor> result = new ArrayList<ToolDescriptor>();
        for (ToolDescriptor descriptor : tools) {
            if (descriptor.isSingleton() && objects.size() > 1) {
                continue;
            }
            boolean applies = true;
            for (DBSObject object : objects) {
                if (!descriptor.appliesTo(object)) {
                    applies = false;
                    break;
                }
            }
            if (applies) {
                result.add(descriptor);
            }
        }
        return result;
    }

}
