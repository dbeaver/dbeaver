/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolBarConfigurationRegistry {
    
    static final String EXTENSION_ID = "org.jkiss.dbeaver.toolBarConfiguration"; //$NON-NLS-1$
    
    private static final Log log = Log.getLog(ToolBarConfigurationRegistry.class);

    private static ToolBarConfigurationRegistry instance = null;

    /**
     * Returns an instance of this singleton
     */
    public static synchronized ToolBarConfigurationRegistry getInstance() {
        if (instance == null) {
            instance = new ToolBarConfigurationRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, ToolBarConfigurationDescriptor> knownToolBars = new LinkedHashMap<>();

    private ToolBarConfigurationRegistry() {
    }

    private void loadExtensions(IExtensionRegistry registry) {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(EXTENSION_ID);
        
        Map<String, List<IConfigurationElement>> toolBarElementsByKey = Stream.of(extConfigs)
            .filter(e -> ToolBarConfigurationDescriptor.TOOLBAR_ELEMENT_NAME.equals(e.getName()))
            .collect(Collectors.groupingBy(e -> e.getAttribute(ToolBarConfigurationDescriptor.KEY_ATTR_NAME)));

        knownToolBars.clear();
        for (Map.Entry<String, List<IConfigurationElement>> entry : toolBarElementsByKey.entrySet()) {
            knownToolBars.put(entry.getKey(), new ToolBarConfigurationDescriptor(entry.getKey(), entry.getValue()));
        }
    }

    public Collection<ToolBarConfigurationDescriptor> getKnownToolBars() {
        return knownToolBars.values();
    }

    /**
     * Checks if item on the toolbar visible
     */    public boolean isItemVisible(@NotNull String toolBarKey, @NotNull String itemKey) {
        ToolBarConfigurationDescriptor toolBar = knownToolBars.get(toolBarKey);
        if (toolBar != null) {
            return toolBar.isItemVisible(itemKey);
        } else {
            log.error("Unknown toolbar key " + toolBarKey + " for item " + itemKey);
            return false;
        }
    }

    static String makeItemVisibilityPreferenceKeyName(@NotNull String toolBarKey, @NotNull String itemKey, @NotNull String property) {
        return String.join(".", new String[]{
            "org.jkiss.dbeaver.toolBarConfiguration", "toolbars", "[" + toolBarKey + "]", "items", "[" + itemKey + "]", property 
        });
    }
}
