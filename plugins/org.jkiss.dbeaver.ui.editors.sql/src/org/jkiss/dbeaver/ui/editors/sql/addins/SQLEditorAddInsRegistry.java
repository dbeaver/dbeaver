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
package org.jkiss.dbeaver.ui.editors.sql.addins;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Configuration registry of the SQL Editor Add-ins
 */
public class SQLEditorAddInsRegistry {
    
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sql.editorAddIns"; //$NON-NLS-1$

    private static final String TAG_VIEW = "addIn"; //$NON-NLS-1$

    private static SQLEditorAddInsRegistry instance;

    /**
     * Returns instance of SQLEditorAddInsRegistry
     */
    public static synchronized SQLEditorAddInsRegistry getInstance() {
        if (instance == null) {
            instance = new SQLEditorAddInsRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<SQLEditorAddInDescriptor> addInDescriptors;

    private SQLEditorAddInsRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] addInElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        
        List<SQLEditorAddInDescriptor> addInDescriptors = new ArrayList<>();
        for (IConfigurationElement ext : addInElements) {
            if (TAG_VIEW.equals(ext.getName())) {
                SQLEditorAddInDescriptor descriptor = new SQLEditorAddInDescriptor(ext);
                addInDescriptors.add(descriptor);
            }
        }
        addInDescriptors.sort(Comparator.comparingInt(SQLEditorAddInDescriptor::getPriority));
        this.addInDescriptors = List.copyOf(addInDescriptors);
    }

    /**
     * A list of editor add-ins in the order of initialization according to their priorities
     */
    public List<SQLEditorAddInDescriptor> getAddIns() {
        return addInDescriptors;
    }
    
}

