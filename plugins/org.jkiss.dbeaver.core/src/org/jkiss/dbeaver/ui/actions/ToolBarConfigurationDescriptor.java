/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ToolBarConfigurationDescriptor {
    
    public class Item {
        private final String key;
        private final String name;
        private final String commandId;
        private final boolean defaultVisibility;
        
        private final String prefKeyVisibility;
        private final String prefKeyIsSet;
        private Boolean isVisible = null;
        
        public Item(@NotNull IConfigurationElement e) {
            String key = e.getAttribute(KEY_ATTR_NAME);
            String name = e.getAttribute(NAME_ATTR_NAME);
            String commandId = e.getAttribute(CMD_ID_ATTR_NAME);
            
            this.key = CommonUtils.isNotEmpty(key) ? key : (CommonUtils.isNotEmpty(commandId) ?  commandId : null);
            this.name = name;
            this.commandId = commandId;
            this.defaultVisibility = CommonUtils.getBoolean(e.getAttribute(DEFAULT_VISIBILITY_ATTR_NAME));
            
            if (this.key == null) {
                throw new RuntimeException("Failed to resolve toolbar configuration item key");
            }
            this.prefKeyVisibility = ToolBarConfigurationRegistry.makeItemVisibilityPreferenceKeyName(
                ToolBarConfigurationDescriptor.this.key, this.key, "visibility"  //$NON-NLS-1$
            );
            this.prefKeyIsSet = ToolBarConfigurationRegistry.makeItemVisibilityPreferenceKeyName(
                ToolBarConfigurationDescriptor.this.key, this.key, "isSet"  //$NON-NLS-1$
            );
        }
        
        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getCommandId() {
            return commandId;
        }
        
        public boolean isVisibleByDefault() {
            return defaultVisibility;
        }

        /**
         * Returns is toolbar item visible
         */
        public boolean isVisible() {
            if (isVisible == null) {
                IPreferenceStore prefs = DBeaverActivator.getInstance().getPreferenceStore();
                isVisible = prefs.getBoolean(prefKeyIsSet) ? prefs.getBoolean(prefKeyVisibility) : defaultVisibility;
            }
            return isVisible;
        }

        /**
         * Set visibility for toolbar item
         */
        public void setVisible(boolean value) {
            IPreferenceStore prefs = DBeaverActivator.getInstance().getPreferenceStore();
            prefs.setValue(prefKeyIsSet, true);
            prefs.setValue(prefKeyVisibility, value);
            isVisible = value;
        }
    }
    
    private static final Log log = Log.getLog(ToolBarConfigurationDescriptor.class);

    static final String TOOLBAR_ELEMENT_NAME = "toolBar"; //$NON-NLS-1$
    static final String ITEM_ELEMENT_NAME = "item"; //$NON-NLS-1$
    static final String KEY_ATTR_NAME = "key"; //$NON-NLS-1$
    static final String NAME_ATTR_NAME = "name"; //$NON-NLS-1$
    static final String CMD_ID_ATTR_NAME = "commandId"; //$NON-NLS-1$
    static final String DEFAULT_VISIBILITY_ATTR_NAME = "defaultVisibility"; //$NON-NLS-1$

    private final String key;
    private final String name;
    
    private final LinkedHashMap<String, Item> itemsByKey;
    
    public ToolBarConfigurationDescriptor(@NotNull String key, @NotNull List<IConfigurationElement> elements) {
        this.key = key;
        
        this.name = elements.stream().map(e -> e.getAttribute(NAME_ATTR_NAME))
            .filter(CommonUtils::isNotEmpty)
            .findFirst().orElse(key);
        
        this.itemsByKey = elements.stream()
            .flatMap(e -> Stream.of(e.getChildren(ITEM_ELEMENT_NAME)))
            .map(e -> new Item(e))
            .collect(Collectors.toMap(
                item -> item.getKey(),
                item -> item,
                (a, b) -> {
                    String msg = "Duplicate toolbar " + key + " configuration item " + a.key;
                    log.error(msg);
                    throw new RuntimeException(msg);
                },
                LinkedHashMap::new));
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns if item visible
     */
    public boolean isItemVisible(@NotNull String itemKey) {
        Item item = itemsByKey.get(itemKey);
        if (item != null) {
            return item.isVisible();
        } else {
            log.error("Testing unknown item key " + itemKey + " for toolbar configuration " + key);
            return false;
        }
    }

    /**
     * Return known toolbar items keys
     */
    @NotNull
    public Collection<Item> getItems() {
        return itemsByKey.values();
    }

}
