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
package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ERDNotationRegistry {

    private Log log = Log.getLog(ERDNotationRegistry.class);
    private Map<String, ERDNotationDescriptor> notations = new LinkedHashMap<>();
    private ERDNotationDescriptor defaultNotation;
    private static ERDNotationRegistry instance;
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.erd.ui.notation.style";

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
    public List<ERDNotationDescriptor> getERDNotations() {
        return notations.values().stream().collect(Collectors.toList());
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
        if (descriptor.isDefault()) {
            if (defaultNotation == null) {
                defaultNotation = descriptor;
            } else {
                log.error("The default ERD Notation already defined for id:" + defaultNotation.getId());
            }
        }
    }

    /**
     * Get notation by identifier
     *
     *@param id - notation descriptor identifier
     *@return - ERDNotationDescriptor
     */
    @Nullable
    public ERDNotationDescriptor getNotation(@NotNull String id) {
        if (!notations.containsKey(id)) {
            log.error("ERD Notation not defined for key:" + id);
            return null;
        }
        return notations.get(id);
    }

    @Nullable
    public ERDNotationDescriptor getDefaultNotation() {
        return this.defaultNotation;
    }

    /**
     * Return notation from registry by name
     *
     * @param name - notation name
     * @return - ERDNotationDescriptor
     */
    @Nullable
    public ERDNotationDescriptor getERDNotationByName(String name) {
        for (ERDNotationDescriptor descriptor : notations.values()) {
            if (descriptor.getName().equals(name)) {
                return descriptor;
            }
        }
        return null;
    }
}
