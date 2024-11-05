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
package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ERDNotationRegistry {

    private static final Log log = Log.getLog(ERDNotationRegistry.class);
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.erd.ui.notation.style";
    private static ERDNotationRegistry instance;
    private final Map<String, ERDNotationDescriptor> notations = new LinkedHashMap<>();
    private ERDNotationDescriptor activeDescriptor;

    private ERDNotationRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] cfgElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement cfe : cfgElements) {
            try {
                addNotation(new ERDNotationDescriptor(cfe));
            } catch (CoreException e) {
                log.error(e.getStatus());
            }
        }
    }

    private DBPPreferenceStore getPreferenceStore() {
        return ERDUIActivator.getDefault().getPreferences();
    }


    /**
     * Registry instance
     *
     * @return - registry instance
     */
    public static synchronized ERDNotationRegistry getInstance() {
        if (instance == null) {
            instance = new ERDNotationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<ERDNotationDescriptor> getNotations() {
        return new ArrayList<>(notations.values());
    }

    /**
     * Add new notation
     *
     * @param descriptor - notation descriptor
     */
    private void addNotation(@NotNull ERDNotationDescriptor descriptor) {
        if (notations.containsKey(descriptor.getId())) {
            log.error("ER Diagram Notation already defined for id:" + descriptor.getId());
            return;
        }
        notations.put(descriptor.getId(), descriptor);
    }

    /**
     * Get notation by identifier
     *
     * @param id - notation descriptor identifier
     * @return - ERDNotationDescriptor
     */
    @Nullable
    public ERDNotationDescriptor getDescriptor(@NotNull String id) {
        if (!notations.containsKey(id)) {
            log.error("ERD Notation not defined for key:" + id);
            return null;
        }
        return notations.get(id);
    }

    /**
     * Return notation from registry by name
     *
     * @param name - notation name
     * @return - ERDNotationDescriptor
     */
    @Nullable
    public ERDNotationDescriptor getDescriptorByName(String name) {
        for (ERDNotationDescriptor descriptor : notations.values()) {
            if (descriptor.getName().equals(name)) {
                return descriptor;
            }
        }
        return null;
    }

    /**
     * The method designed to retrieve stored value of notation from configuration
     * scope
     *
     * @return - descriptor
     */
    public ERDNotationDescriptor getActiveDescriptor() {
        if (activeDescriptor != null) {
            return activeDescriptor;
        }
        activeDescriptor = getDescriptor(getPreferenceStore().getString(ERDUIConstants.PREF_NOTATION_TYPE));
        if (activeDescriptor != null) {
            return activeDescriptor;
        }
        activeDescriptor = getDefaultDescriptor();
        return activeDescriptor;
    }

    /**
     * The method designed to set and store current descriptor 
     */
    public void setActiveDescriptor(ERDNotationDescriptor erdNotation) {
        activeDescriptor = erdNotation;
        getPreferenceStore().setValue(ERDUIConstants.PREF_NOTATION_TYPE, activeDescriptor.getId());
    }
    
    /**
     * Gets default 
     *
     * @return - default descriptor
     */
    public ERDNotationDescriptor getDefaultDescriptor() {
        return getDescriptor(ERDUIConstants.PREF_DEFAULT_ATTR_ERD_NOTATION_ID);
    }
}
