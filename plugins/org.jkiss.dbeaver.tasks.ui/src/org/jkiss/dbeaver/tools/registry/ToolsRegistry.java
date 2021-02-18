/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

    public List<ToolDescriptor> getTools() {
        return tools;
    }

}
