/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.*;

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
        List<ToolDescriptor> result = new ArrayList<>();
        if (!objects.isEmpty()) {
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
        }
        return result;
    }

    public boolean hasTools(IStructuredSelection selection) {
        boolean singleObject = selection.size() == 1;
        for (Iterator iter = selection.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            DBSObject dbObject = DBUtils.getFromObject(item);
            if (dbObject != null) {
                item = dbObject;
            }
            if (item instanceof DBPObject) {
                for (ToolDescriptor descriptor : tools) {
                    if (descriptor.isSingleton() && !singleObject) {
                        continue;
                    }
                    if (descriptor.appliesTo((DBPObject) item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
