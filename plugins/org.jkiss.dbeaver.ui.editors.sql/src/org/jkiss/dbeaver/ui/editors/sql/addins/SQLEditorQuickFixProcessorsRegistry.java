/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SQLEditorQuickFixProcessorsRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sql.quickFixProcessors"; //$NON-NLS-1$

    private static final String TAG_QUICK_FIX_PROCESSOR = "quickFixProcessor"; //$NON-NLS-1$

    @Nullable
    private static SQLEditorQuickFixProcessorsRegistry instance = null;

    @NotNull
    private final Collection<SQLEditorQuickFixProcessorDescriptor> quickFixProcDescriptors;

    /**
     * Returns instance of SQLEditorAddInsRegistry
     */
    public static synchronized SQLEditorQuickFixProcessorsRegistry getInstance() {
        if (instance == null) {
            instance = new SQLEditorQuickFixProcessorsRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private SQLEditorQuickFixProcessorsRegistry(@NotNull IExtensionRegistry registry) {
        Map<Class<?>, SQLEditorQuickFixProcessorDescriptor> descs = new HashMap<>();
        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if (TAG_QUICK_FIX_PROCESSOR.equals(element.getName())) {
                SQLEditorQuickFixProcessorDescriptor desc = new SQLEditorQuickFixProcessorDescriptor((element));
                descs.put(desc.getClass(), desc);
            }
        }

        this.quickFixProcDescriptors = descs.values();
    }

    @NotNull
    public Collection<SQLEditorQuickFixProcessorDescriptor> getQuickFixProcessorDescriptors() {
        return this.quickFixProcDescriptors;
    }
}
